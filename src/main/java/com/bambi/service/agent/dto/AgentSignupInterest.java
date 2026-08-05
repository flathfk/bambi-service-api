package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * agent-api {@code signup_interests} 항목.
 *
 * <p>taxonomy 토픽은 정식 Category 이름을 전달하고, 사용자가 직접 입력한 토픽 묶음은
 * Category가 없으므로 {@code null}을 전달한다.
 */
public record AgentSignupInterest(
        @JsonProperty("category") String category,
        @JsonProperty("topics") List<String> topics) {

    public AgentSignupInterest {
        topics = List.copyOf(topics);
    }
}
