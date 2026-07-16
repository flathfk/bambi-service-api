package com.bambi.service.agent.dto;

import java.util.List;

/**
 * Agent 계약: POST /agent/cards/generate 요청.
 * docs/agent-contract.md 와 필드를 1:1 로 유지한다 (Contract Test 대상).
 * collectedItems 는 RSS 수집(P1) 전까지 항상 빈 배열.
 */
public record CardGenerateRequest(
        Long userId,
        List<String> interests,
        List<BookmarkPayload> bookmarks,
        List<Object> collectedItems) {

    /** 카드 생성 재료로 넘기는 북마크 요약본 */
    public record BookmarkPayload(String title, String summary, String url) {
    }
}
