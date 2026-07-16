package com.bambi.service.bookmark.dto;

import com.bambi.service.card.dto.CardResponse;

/**
 * 저장 응답 — P0 합의안 "즉시 카드 1장": 저장 결과와 함께 방금 생성된 카드를 돌려준다.
 * (프론트는 저장 직후 이 카드를 바로 보여줄 수 있다)
 */
public record BookmarkCreateResponse(
        BookmarkResponse bookmark,
        CardResponse card) {
}
