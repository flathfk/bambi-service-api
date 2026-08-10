package com.bambi.service.agent.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Agent Job 상태 Batch 응답의 한 항목. */
public record AgentJobStatus(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("job_type") String jobType,
        String status,
        Integer progress,
        @JsonProperty("error_code") String errorCode) {
}
