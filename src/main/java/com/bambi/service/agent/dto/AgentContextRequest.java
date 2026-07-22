package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * agent-api 사용자 컨텍스트 upsert 요청 바디.
 * 계약: PUT /internal/v1/users/{userId}/context (agent UserContextUpsertRequest 와 1:1, snake_case).
 *
 * @param contextVersion         사용자별 단조 증가 정수. 같거나 작으면 agent 가 STALE_CONTEXT_VERSION 반환
 * @param plan                   요금제 (free | paid)
 * @param preferredLanguage      선호 언어 (기본 ko)
 * @param personalizationEnabled 개인화 사용 여부
 * @param blockedInterestIds     사용자가 차단(삭제)한 관심사 ID 목록
 * @param blockedSourceIds       사용자가 차단(삭제)한 소스 ID 목록
 */
public record AgentContextRequest(
        @JsonProperty("context_version") int contextVersion,
        @JsonProperty("plan") String plan,
        @JsonProperty("preferred_language") String preferredLanguage,
        @JsonProperty("personalization_enabled") boolean personalizationEnabled,
        @JsonProperty("blocked_interest_ids") List<String> blockedInterestIds,
        @JsonProperty("blocked_source_ids") List<String> blockedSourceIds) {

    /** 회원가입 직후 최초 동기화용 기본 컨텍스트 (버전 1, 무료, 한국어, 개인화 on, 차단 없음). */
    public static AgentContextRequest initialForSignup() {
        return new AgentContextRequest(1, "free", "ko", true, List.of(), List.of());
    }
}
