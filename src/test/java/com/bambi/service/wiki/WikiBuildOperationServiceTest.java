package com.bambi.service.wiki;

import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.agent.jobs.AgentJobResult;
import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.wiki.dto.WikiBuildStatusResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Wiki 빌드 등록·Job 체인 전환·사용자 집계 상태를 검증한다. */
class WikiBuildOperationServiceTest {

    private final WikiBuildOperationRepository repository = mock(WikiBuildOperationRepository.class);
    private final WikiBuildOperationService service = new WikiBuildOperationService(repository);

    @Test
    void Agent_접수를_결정적_ID로_멱등_등록한다() {
        service.register(7L, "bookmark-42", new AgentAcceptedJob("job-1", "queued"));

        verify(repository).upsertAccepted(any(UUID.class), eq(7L), eq("bookmark-42"), eq("job-1"));
    }

    @Test
    void URL_수집_완료시_결과의_Wiki_Job으로_추적을_이어간다() {
        WikiBuildOperation operation = operation("job-url", "RUNNING", OffsetDateTime.now());
        AgentJobStatusClient client = mock(AgentJobStatusClient.class);
        when(client.getResult("job-url")).thenReturn(new AgentJobResult(
                "job-url", "completed", Map.of("wiki_build_job_id", "job-wiki")));

        service.applyStatus(operation,
                new AgentJobStatus("job-url", "personal_wiki_url", "completed", 100, null), client);

        verify(repository).advanceToWikiJob(operation.getId(), "job-wiki");
    }

    @Test
    void Wiki_Build_완료는_작업을_완료한다() {
        WikiBuildOperation operation = operation("job-wiki", "RUNNING", OffsetDateTime.now());

        service.applyStatus(operation,
                new AgentJobStatus("job-wiki", "personal_wiki_build", "completed", 100, null),
                mock(AgentJobStatusClient.class));

        verify(repository).updateStatus(operation.getId(), "COMPLETED", null);
    }

    @Test
    void 활성_작업이_있으면_BUILDING으로_집계한다() {
        WikiBuildOperation latest = operation("job-1", "RUNNING", OffsetDateTime.now());
        when(repository.countByUserIdAndStatusIn(7L, List.of("PENDING", "RUNNING"))).thenReturn(2L);
        when(repository.findFirstByUserIdOrderByUpdatedAtDesc(7L)).thenReturn(Optional.of(latest));

        WikiBuildStatusResponse response = service.statusFor(7L);

        assertThat(response.status()).isEqualTo("BUILDING");
        assertThat(response.activeCount()).isEqualTo(2);
    }

    private static WikiBuildOperation operation(String jobId, String status, OffsetDateTime updatedAt) {
        WikiBuildOperation operation = mock(WikiBuildOperation.class);
        when(operation.getId()).thenReturn(UUID.randomUUID());
        when(operation.getCurrentAgentJobId()).thenReturn(jobId);
        when(operation.getStatus()).thenReturn(status);
        when(operation.getUpdatedAt()).thenReturn(updatedAt);
        return operation;
    }
}
