package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Agent 비동기 작업 202 접수 응답에서 상태 추적에 필요한 최소 필드. */
public record AgentAcceptedJob(
        @JsonProperty("job_id") String jobId,
        String status) {
}
