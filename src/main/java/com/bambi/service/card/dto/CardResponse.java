package com.bambi.service.card.dto;

import com.bambi.service.card.Card;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 카드 응답 DTO — Entity 직접 노출 금지.
 * 대외 식별자는 publicId(UUID) 만 노출한다 (내부 순번 id 숨김).
 * reportId = 이 카드의 본문(리포트) publicId. 프론트가 카드 상세 → 본문(GET /api/reports/{reportId})
 * 으로 이동하는 진입점. 동기 즉시 카드처럼 리포트가 없으면 null.
 */
public record CardResponse(
        UUID publicId,
        UUID reportId,
        String title,
        String summary,
        String whyForYou,
        List<SourceResponse> sources,
        OffsetDateTime createdAt) {

    public record SourceResponse(String title, String url) {
    }

    /** 리포트 없는(또는 참조 불필요한) 카드 — reportId=null. */
    public static CardResponse from(Card card) {
        return from(card, null);
    }

    /** reportPublicId = 이 카드가 참조하는 리포트의 publicId(없으면 null). */
    public static CardResponse from(Card card, UUID reportPublicId) {
        List<SourceResponse> sources = card.getSources().stream()
                .map(s -> new SourceResponse(s.getTitle(), s.getUrl()))
                .toList();
        return new CardResponse(
                card.getPublicId(),
                reportPublicId,
                card.getTitle(),
                card.getSummary(),
                card.getWhyForYou(),
                sources,
                card.getCreatedAt());
    }
}
