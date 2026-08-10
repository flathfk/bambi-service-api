package com.bambi.service.worker;

import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusBatchResponse;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.generation.GenerationPending;
import com.bambi.service.generation.GenerationPendingService;
import com.bambi.service.wiki.WikiBuildOperation;
import com.bambi.service.wiki.WikiBuildOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 활성 Wiki·Report 작업의 Agent Job 상태를 한 Batch로 주기적으로 동기화한다. */
@Component
@ConditionalOnProperty(name = "app.worker.job-status.enabled", havingValue = "true", matchIfMissing = true)
public class AgentJobStatusPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentJobStatusPollingWorker.class);

    private final WikiBuildOperationService wikiOperations;
    private final GenerationPendingService generationPendings;
    private final AgentJobStatusClient statusClient;

    @Value("${app.worker.job-status.batch-limit:100}")
    private int batchLimit;

    public AgentJobStatusPollingWorker(
            WikiBuildOperationService wikiOperations,
            GenerationPendingService generationPendings,
            AgentJobStatusClient statusClient) {
        this.wikiOperations = wikiOperations;
        this.generationPendings = generationPendings;
        this.statusClient = statusClient;
    }

    /** 활성 작업이 있을 때만 Agent Batch API를 호출하고 작업별로 독립 반영한다. */
    @Scheduled(fixedDelayString = "${app.worker.job-status.poll-interval-ms:5000}",
            initialDelayString = "${app.worker.job-status.initial-delay-ms:5000}")
    public void poll() {
        List<WikiBuildOperation> wiki = wikiOperations.findPollable(batchLimit);
        List<GenerationPending> generations = generationPendings.findPollable(batchLimit);
        Map<String, WikiBuildOperation> wikiByJob = new LinkedHashMap<>();
        Map<String, GenerationPending> generationByJob = new LinkedHashMap<>();
        int index = 0;
        while (wikiByJob.size() + generationByJob.size() < batchLimit
                && (index < wiki.size() || index < generations.size())) {
            if (index < wiki.size()) {
                WikiBuildOperation operation = wiki.get(index);
                wikiByJob.putIfAbsent(operation.getCurrentAgentJobId(), operation);
            }
            if (wikiByJob.size() + generationByJob.size() < batchLimit && index < generations.size()) {
                GenerationPending pending = generations.get(index);
                generationByJob.putIfAbsent(pending.getAgentJobId(), pending);
            }
            index++;
        }
        List<String> jobIds = java.util.stream.Stream.concat(
                        wikiByJob.keySet().stream(), generationByJob.keySet().stream())
                .distinct()
                .toList();
        if (jobIds.isEmpty()) {
            return;
        }

        AgentJobStatusBatchResponse response;
        try {
            response = statusClient.getStatuses(jobIds);
        } catch (Exception e) {
            log.warn("[AgentJobStatus] Batch 조회 실패 — 다음 tick 재시도", e);
            return;
        }
        for (String missing : response.missingJobIds()) {
            if (wikiByJob.containsKey(missing)) wikiOperations.markMissing(wikiByJob.get(missing));
            if (generationByJob.containsKey(missing)) generationPendings.markMissing(generationByJob.get(missing));
        }
        for (AgentJobStatus status : response.items()) {
            WikiBuildOperation wikiOperation = wikiByJob.get(status.jobId());
            GenerationPending generation = generationByJob.get(status.jobId());
            try {
                if (wikiOperation != null) wikiOperations.applyStatus(wikiOperation, status, statusClient);
                if (generation != null) generationPendings.applyAgentStatus(generation, status);
            } catch (Exception e) {
                log.warn("[AgentJobStatus] 상태 반영 실패 jobId={} — 다음 tick 재시도", status.jobId(), e);
            }
        }
    }
}
