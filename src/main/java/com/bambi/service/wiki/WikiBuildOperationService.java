package com.bambi.service.wiki;

import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.agent.jobs.AgentJobResult;
import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.wiki.dto.WikiBuildStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Wiki 원천 접수부터 최종 Wiki Build 완료까지 Agent Job 체인을 추적한다. */
@Service
public class WikiBuildOperationService {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "RUNNING");
    private static final Duration FAILURE_VISIBLE_WINDOW = Duration.ofHours(1);

    private final WikiBuildOperationRepository repository;

    public WikiBuildOperationService(WikiBuildOperationRepository repository) {
        this.repository = repository;
    }

    /** Agent가 접수한 Wiki 원천 Job을 사용자 작업으로 멱등 등록한다. */
    public void register(long userId, String sourceEventId, AgentAcceptedJob accepted) {
        UUID id = UUID.nameUUIDFromBytes((userId + ":" + sourceEventId).getBytes(StandardCharsets.UTF_8));
        repository.upsertAccepted(id, userId, sourceEventId, accepted.jobId());
    }

    /** 이번 tick에서 조회할 활성 Wiki 작업을 반환한다. */
    @Transactional(readOnly = true)
    public List<WikiBuildOperation> findPollable(int limit) {
        return repository.findPollable(limit);
    }

    /** Agent 상태를 Service의 Wiki 작업 상태로 전환한다. */
    public void applyStatus(
            WikiBuildOperation operation,
            AgentJobStatus status,
            AgentJobStatusClient client) {
        switch (status.status()) {
            case "queued" -> repository.updateStatus(operation.getId(), "PENDING", null);
            case "running" -> repository.updateStatus(operation.getId(), "RUNNING", null);
            case "failed" -> repository.updateStatus(operation.getId(), "FAILED", status.errorCode());
            case "cancelled" -> repository.updateStatus(operation.getId(), "CANCELLED", status.errorCode());
            case "completed" -> completeOrAdvance(operation, status, client);
            default -> repository.updateStatus(operation.getId(), "FAILED", "UNKNOWN_JOB_STATUS");
        }
    }

    /** Agent에서 찾지 못한 활성 Job을 최종 실패로 닫는다. */
    public void markMissing(WikiBuildOperation operation) {
        repository.updateStatus(operation.getId(), "FAILED", "JOB_NOT_FOUND");
    }

    /** 사용자 화면에 빌드 중·실패·유휴 집계 상태를 반환한다. */
    @Transactional(readOnly = true)
    public WikiBuildStatusResponse statusFor(long userId) {
        long activeCount = repository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
        WikiBuildOperation latest = repository.findFirstByUserIdOrderByUpdatedAtDesc(userId).orElse(null);
        if (activeCount > 0) {
            return new WikiBuildStatusResponse(
                    "BUILDING", activeCount, latest != null ? latest.getUpdatedAt() : null, null);
        }
        if (latest != null
                && List.of("FAILED", "CANCELLED").contains(latest.getStatus())
                && latest.getUpdatedAt().isAfter(OffsetDateTime.now().minus(FAILURE_VISIBLE_WINDOW))) {
            return new WikiBuildStatusResponse("FAILED", 0, latest.getUpdatedAt(), latest.getErrorCode());
        }
        return new WikiBuildStatusResponse("IDLE", 0, latest != null ? latest.getUpdatedAt() : null, null);
    }

    /** URL 수집이면 후속 Wiki Job으로 이동하고, Wiki Build 자체면 완료한다. */
    private void completeOrAdvance(
            WikiBuildOperation operation,
            AgentJobStatus status,
            AgentJobStatusClient client) {
        if ("personal_wiki_url".equals(status.jobType())) {
            AgentJobResult result = client.getResult(status.jobId());
            String wikiBuildJobId = result.stringValue("wiki_build_job_id");
            if (wikiBuildJobId == null) {
                repository.updateStatus(operation.getId(), "FAILED", "WIKI_BUILD_JOB_ID_MISSING");
                return;
            }
            repository.advanceToWikiJob(operation.getId(), wikiBuildJobId);
            return;
        }
        repository.updateStatus(operation.getId(), "COMPLETED", null);
    }
}
