package com.bambi.service.agent.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Agent Job 상태 Batch 조회 결과와 누락된 식별자. */
public record AgentJobStatusBatchResponse(
        List<AgentJobStatus> items,
        @JsonProperty("missing_job_ids") List<String> missingJobIds) {

    /** null 배열을 빈 배열로 정규화한다. */
    public AgentJobStatusBatchResponse {
        items = items != null ? List.copyOf(items) : List.of();
        missingJobIds = missingJobIds != null ? List.copyOf(missingJobIds) : List.of();
    }
}
