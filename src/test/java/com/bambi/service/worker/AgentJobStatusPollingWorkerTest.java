package com.bambi.service.worker;

import com.bambi.service.agent.jobs.AgentJobStatus;
import com.bambi.service.agent.jobs.AgentJobStatusBatchResponse;
import com.bambi.service.agent.jobs.AgentJobStatusClient;
import com.bambi.service.generation.GenerationPending;
import com.bambi.service.generation.GenerationPendingService;
import com.bambi.service.wiki.WikiBuildOperation;
import com.bambi.service.wiki.WikiBuildOperationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Wiki·Report 활성 Job을 한 Batch로 조회해 각 상태에 반영하는지 검증한다. */
class AgentJobStatusPollingWorkerTest {

    @Test
    void 활성_Wiki와_Report_작업을_한번에_조회해_상태를_반영한다() {
        WikiBuildOperationService wikiOperations = mock(WikiBuildOperationService.class);
        GenerationPendingService generationPendings = mock(GenerationPendingService.class);
        AgentJobStatusClient client = mock(AgentJobStatusClient.class);
        WikiBuildOperation wiki = mock(WikiBuildOperation.class);
        GenerationPending report = mock(GenerationPending.class);
        when(wiki.getCurrentAgentJobId()).thenReturn("job-wiki");
        when(report.getAgentJobId()).thenReturn("job-report");
        when(wikiOperations.findPollable(100)).thenReturn(List.of(wiki));
        when(generationPendings.findPollable(100)).thenReturn(List.of(report));
        AgentJobStatus wikiStatus = new AgentJobStatus(
                "job-wiki", "personal_wiki_build", "running", 5, null);
        AgentJobStatus reportStatus = new AgentJobStatus(
                "job-report", "report_generation", "completed", 100, null);
        when(client.getStatuses(List.of("job-wiki", "job-report")))
                .thenReturn(new AgentJobStatusBatchResponse(
                        List.of(wikiStatus, reportStatus), List.of()));
        AgentJobStatusPollingWorker worker = new AgentJobStatusPollingWorker(
                wikiOperations, generationPendings, client);
        ReflectionTestUtils.setField(worker, "batchLimit", 100);

        worker.poll();

        verify(wikiOperations).applyStatus(wiki, wikiStatus, client);
        verify(generationPendings).applyAgentStatus(report, reportStatus);
    }
}
