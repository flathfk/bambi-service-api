package com.bambi.service.like.dto;

/**
 * 좋아요/취소 결과 — 프론트 낙관적 업데이트의 확정값(내 좋아요 상태 + 카드의 좋아요 총수).
 */
public record LikeResponse(
        boolean liked,
        long likeCount) {
}
