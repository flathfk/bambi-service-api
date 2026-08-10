package com.bambi.service.agent.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** 완료된 Agent Job의 기능별 결과. */
public record AgentJobResult(
        @JsonProperty("job_id") String jobId,
        String status,
        Map<String, Object> result) {

    /** 결과 문자열 필드를 안전하게 읽는다. */
    public String stringValue(String key) {
        Object value = result != null ? result.get(key) : null;
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
