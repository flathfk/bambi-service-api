package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * agent 관심 Profile 조회 중계 응답 (GET /api/wiki/tags).
 *
 * <p>agent {@code InterestProfileResponse}(snake_case)를 그대로 읽어 camelCase 로 노출한다.
 * 팀 결정(07-29): agent가 자동 추출한 관심 키워드는 <b>tag</b>(해시태그 성격)로 명칭한다 →
 * agent 필드 {@code topic}·{@code interest_id} 를 {@code tag}·{@code tagId} 로 리네임해 내려준다.
 * (온보딩에서 사용자가 직접 고르는 "topic"과는 별개 개념 — 그건 service 직접 관심사)
 */
public record WikiTagsResponse(
        @JsonAlias("profile_id") String profileId,
        int version,
        String status,
        @JsonAlias("calculated_at") String calculatedAt,
        @JsonAlias("interests") List<WikiTag> tags) {

    /** 아직 활성 관심 Profile이 없는 사용자(agent 404) — 빈 목록으로 정규화한다. */
    public static WikiTagsResponse empty() {
        return new WikiTagsResponse(null, 0, "empty", null, List.of());
    }
}
