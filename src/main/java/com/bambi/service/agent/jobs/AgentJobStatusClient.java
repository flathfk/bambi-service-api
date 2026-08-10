package com.bambi.service.agent.jobs;

import java.util.List;

/** Service Worker가 Agent Job 상태와 완료 결과를 조회하는 HTTP 경계. */
public interface AgentJobStatusClient {

    /** 활성 Agent Job 여러 건의 상태를 한 번에 조회한다. */
    AgentJobStatusBatchResponse getStatuses(List<String> jobIds);

    /** 완료된 Agent Job 하나의 결과를 조회한다. */
    AgentJobResult getResult(String jobId);
}
