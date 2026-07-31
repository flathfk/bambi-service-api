package com.bambi.service.scrap.dto;

import com.bambi.service.card.Card;
import com.bambi.service.user.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 내 스크랩 목록의 한 항목 — 담아둔 공개 카드의 요약·관심사 태그·작성자.
 * 식별자는 모두 publicId(UUID). 본문(리포트)은 별도(GET /api/reports/{...}) 조회.
 */
public record ScrapCardResponse(
        UUID publicId,
        String title,
        String summary,
        List<String> tags,
        AuthorResponse author,
        OffsetDateTime createdAt) {

    public record AuthorResponse(UUID publicId, String username, String displayName) {

        static AuthorResponse from(User user) {
            if (user == null) {
                return new AuthorResponse(null, null, null);
            }
            return new AuthorResponse(user.getPublicId(), user.getUsername(), user.getDisplayName());
        }
    }

    public static ScrapCardResponse from(Card card, User author) {
        return new ScrapCardResponse(
                card.getPublicId(),
                card.getTitle(),
                card.getSummary(),
                List.copyOf(card.getInterestTags()),
                AuthorResponse.from(author),
                card.getCreatedAt());
    }
}
