package com.bambi.service.bookmark.dto;

import com.bambi.service.bookmark.Bookmark;
import com.bambi.service.bookmark.BookmarkStatus;

import java.time.OffsetDateTime;

/** 북마크 상세 — 목록과 달리 원문(content)까지 포함 */
public record BookmarkDetailResponse(
        Long id,
        String url,
        String title,
        String content,
        String summary,
        BookmarkStatus status,
        OffsetDateTime createdAt) {

    public static BookmarkDetailResponse from(Bookmark bookmark) {
        return new BookmarkDetailResponse(
                bookmark.getId(),
                bookmark.getUrl(),
                bookmark.getTitle(),
                bookmark.getContent(),
                bookmark.getSummary(),
                bookmark.getStatus(),
                bookmark.getCreatedAt());
    }
}
