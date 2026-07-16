package com.bambi.service.card.dto;

import com.bambi.service.card.Card;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 카드 응답 DTO — Entity 직접 노출 금지.
 * 대외 식별자는 publicId(UUID) 만 노출한다 (내부 순번 id 숨김).
 */
public record CardResponse(
        UUID publicId,
        String title,
        String summary,
        String whyForYou,
        List<SourceResponse> sources,
        OffsetDateTime createdAt) {

    public record SourceResponse(String title, String url) {
    }

    public static CardResponse from(Card card) {
        List<SourceResponse> sources = card.getSources().stream()
                .map(s -> new SourceResponse(s.getTitle(), s.getUrl()))
                .toList();
        return new CardResponse(
                card.getPublicId(),
                card.getTitle(),
                card.getSummary(),
                card.getWhyForYou(),
                sources,
                card.getCreatedAt());
    }
}
