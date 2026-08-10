package com.bambi.service.agent.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Agent Job 상태 Batch 조회 요청. */
public record AgentJobStatusBatchRequest(
        @JsonProperty("job_ids") List<String> jobIds) {
}
