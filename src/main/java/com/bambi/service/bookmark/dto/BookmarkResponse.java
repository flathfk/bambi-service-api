package com.bambi.service.bookmark.dto;

import com.bambi.service.bookmark.Bookmark;
import com.bambi.service.bookmark.BookmarkStatus;

import java.time.OffsetDateTime;

public record BookmarkResponse(
        Long id,
        String url,
        String title,
        String summary,
        BookmarkStatus status,
        OffsetDateTime createdAt) {

    public static BookmarkResponse from(Bookmark bookmark) {
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getUrl(),
                bookmark.getTitle(),
                bookmark.getSummary(),
                bookmark.getStatus(),
                bookmark.getCreatedAt());
    }
}
