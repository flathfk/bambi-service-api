package com.bambi.service.agent.dto;

/**
 * Agent 계약: POST /agent/bookmarks/process 요청.
 * docs/agent-contract.md 와 필드를 1:1 로 유지한다 (Contract Test 대상).
 */
public record BookmarkProcessRequest(
        Long bookmarkId,
        String title,
        String url,
        String content) {
}
