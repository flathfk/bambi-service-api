package com.bambi.service.agent.dto;

import java.util.List;

/**
 * Agent 계약: POST /agent/bookmarks/process 응답.
 * docs/agent-contract.md 와 필드를 1:1 로 유지한다 (Contract Test 대상).
 */
public record BookmarkProcessResponse(
        String summary,
        List<String> interests,
        List<String> tags,
        double confidence) {
}
