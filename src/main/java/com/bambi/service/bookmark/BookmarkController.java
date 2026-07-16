package com.bambi.service.bookmark;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.bookmark.dto.BookmarkCreateRequest;
import com.bambi.service.bookmark.dto.BookmarkCreateResponse;
import com.bambi.service.bookmark.dto.BookmarkDetailResponse;
import com.bambi.service.bookmark.dto.BookmarkResponse;
import com.bambi.service.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * URL/본문 저장 + 북마크 목록/상세 (P0).
 * 소유자 범위는 @AuthenticationPrincipal 의 userId 로 강제한다 (note 템플릿 패턴).
 */
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /** 저장 — 성공 시 201 + 방금 만들어진 카드 1장을 함께 반환 (즉시 카드) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookmarkCreateResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                      @Valid @RequestBody BookmarkCreateRequest request) {
        return ApiResponse.ok(bookmarkService.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<BookmarkResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(bookmarkService.list(principal.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookmarkDetailResponse> get(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(bookmarkService.get(principal.id(), id));
    }
}
