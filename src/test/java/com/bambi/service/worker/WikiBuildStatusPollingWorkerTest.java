package com.bambi.service.worker;

import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusBatchResponse;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.wiki.WikiBuildOperation;
import com.bambi.service.wiki.WikiBuildOperationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Wiki 상태 폴러가 활성 작업만 Batch 조회하고 결과를 반영하는지 검증한다. */
class WikiBuildStatusPollingWorkerTest {

    @Test
    void 활성_Wiki_작업을_한번에_조회해_상태를_반영한다() {
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        AgentJobStatusClient client = mock(AgentJobStatusClient.class);
        WikiBuildOperation operation = mock(WikiBuildOperation.class);
        when(operation.getCurrentAgentJobId()).thenReturn("job-1");
        when(operations.findPollable(100)).thenReturn(List.of(operation));
        AgentJobStatus status = new AgentJobStatus(
                "job-1", "personal_wiki_build", "running", 5, null);
        when(client.getStatuses(List.of("job-1")))
                .thenReturn(new AgentJobStatusBatchResponse(List.of(status), List.of()));
        WikiBuildStatusPollingWorker worker = new WikiBuildStatusPollingWorker(operations, client);
        ReflectionTestUtils.setField(worker, "batchLimit", 100);

        worker.poll();

        verify(operations).applyStatus(operation, status, client);
    }
}
