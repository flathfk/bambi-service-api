package com.bambi.service.scrap.dto;

/**
 * 스크랩/취소 결과 — 프론트 낙관적 업데이트의 확정값(내 스크랩 상태).
 */
public record ScrapResponse(boolean scrapped) {
}
