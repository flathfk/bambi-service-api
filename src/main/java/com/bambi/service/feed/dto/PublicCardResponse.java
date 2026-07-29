package com.bambi.service.feed.dto;

import com.bambi.service.card.Card;
import com.bambi.service.user.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 공개 피드 카드 응답 (SNS/Week2). 내 피드용 {@code CardResponse} 와 분리해 P0 회귀를 막는다.
 * 작성자(author)·좋아요 수·내 좋아요 여부는 서비스가 배치로 채워 넣는다(카드별 재조회 없음).
 * 식별자는 모두 publicId(UUID) — 순번 id 는 노출하지 않는다.
 */
public record PublicCardResponse(
        UUID publicId,
        String title,
        String summary,
        // TODO(07-27 결정): why_for_you 는 폐기 방향 → 관심사 태그로 대체 예정.
        //   agent 카드/리포트 태그 스키마 확정되면 이 필드를 태그로 교체한다(소라·영현 협의).
        String whyForYou,
        AuthorResponse author,
        long likeCount,
        boolean liked,
        List<SourceResponse> sources,
        OffsetDateTime createdAt) {

    public record AuthorResponse(UUID publicId, String username, String displayName) {

        static AuthorResponse from(User user) {
            if (user == null) {
                return new AuthorResponse(null, null, null);
            }
            return new AuthorResponse(user.getPublicId(), user.getUsername(), user.getDisplayName());
        }
    }

    public record SourceResponse(String title, String url) {
    }

    public static PublicCardResponse from(Card card, User author, long likeCount, boolean liked) {
        List<SourceResponse> sources = card.getSources().stream()
                .map(s -> new SourceResponse(s.getTitle(), s.getUrl()))
                .toList();
        return new PublicCardResponse(
                card.getPublicId(),
                card.getTitle(),
                card.getSummary(),
                card.getWhyForYou(),
                AuthorResponse.from(author),
                likeCount,
                liked,
                sources,
                card.getCreatedAt());
    }
}
