package com.bambi.service.worker;

import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusBatchResponse;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.wiki.WikiBuildOperation;
import com.bambi.service.wiki.WikiBuildOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 활성 Wiki 작업의 Agent Job 상태를 주기적으로 동기화한다. */
@Component
@ConditionalOnProperty(name = "app.worker.job-status.enabled", havingValue = "true", matchIfMissing = true)
public class WikiBuildStatusPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(WikiBuildStatusPollingWorker.class);

    private final WikiBuildOperationService operationService;
    private final AgentJobStatusClient statusClient;

    @Value("${app.worker.job-status.batch-limit:100}")
    private int batchLimit;

    public WikiBuildStatusPollingWorker(
            WikiBuildOperationService operationService,
            AgentJobStatusClient statusClient) {
        this.operationService = operationService;
        this.statusClient = statusClient;
    }

    /** 활성 Wiki Job이 있을 때만 Agent Batch API를 호출한다. */
    @Scheduled(fixedDelayString = "${app.worker.job-status.poll-interval-ms:5000}",
            initialDelayString = "${app.worker.job-status.initial-delay-ms:5000}")
    public void poll() {
        List<WikiBuildOperation> operations = operationService.findPollable(batchLimit);
        if (operations.isEmpty()) {
            return;
        }
        Map<String, WikiBuildOperation> byJobId = operations.stream()
                .collect(Collectors.toMap(
                        WikiBuildOperation::getCurrentAgentJobId,
                        Function.identity(),
                        (first, ignored) -> first));
        AgentJobStatusBatchResponse response;
        try {
            response = statusClient.getStatuses(List.copyOf(byJobId.keySet()));
        } catch (Exception e) {
            log.warn("[WikiBuildStatus] Agent 상태 Batch 조회 실패 — 다음 tick 재시도", e);
            return;
        }
        for (String missing : response.missingJobIds()) {
            WikiBuildOperation operation = byJobId.get(missing);
            if (operation != null) {
                operationService.markMissing(operation);
            }
        }
        for (AgentJobStatus status : response.items()) {
            WikiBuildOperation operation = byJobId.get(status.jobId());
            if (operation == null) {
                continue;
            }
            try {
                operationService.applyStatus(operation, status, statusClient);
            } catch (Exception e) {
                log.warn("[WikiBuildStatus] 상태 반영 실패 jobId={} — 다음 tick 재시도", status.jobId(), e);
            }
        }
    }
}
