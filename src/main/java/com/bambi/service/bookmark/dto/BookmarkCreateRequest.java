package com.bambi.service.bookmark.dto;

import jakarta.validation.constraints.Size;

/**
 * URL/본문 저장 요청. url 과 content 중 최소 하나는 필수 — 조합 검증은 서비스에서 수행.
 */
public record BookmarkCreateRequest(
        @Size(max = 2048) String url,
        @Size(max = 500) String title,
        String content) {

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }
}
