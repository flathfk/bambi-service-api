package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /**
     * 생성 검색 주제로 쓸 대표 관심사(태그) 1개 — score 가 가장 높은 태그.
     * 계약상 생성 요청의 topic 은 라벨이 아니라 <b>실제 검색 주제</b>라, 고정 문구 대신 이 값을 넣는다
     * (유림 확인 08-05). 관심사가 없으면 empty → 호출부가 생성을 거절/건너뛴다.
     */
    public Optional<String> topTopic() {
        if (tags == null) {
            return Optional.empty();
        }
        return tags.stream()
                .filter(t -> t.tag() != null && !t.tag().isBlank())
                .max(Comparator.comparingDouble(WikiTag::score))
                .map(WikiTag::tag);
    }
}
