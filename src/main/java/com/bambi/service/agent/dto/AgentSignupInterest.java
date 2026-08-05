package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * agent-api {@code signup_interests} 항목.
 *
 * <p>현재 service DB에는 온보딩 Category가 없으므로, 사용자 직접 설정 관심사 이름들을
 * 하나의 임시 Category 아래 Topic 목록으로 전달한다.
 */
public record AgentSignupInterest(
        @JsonProperty("category") String category,
        @JsonProperty("topics") List<String> topics) {

    public AgentSignupInterest {
        topics = List.copyOf(topics);
    }
}
