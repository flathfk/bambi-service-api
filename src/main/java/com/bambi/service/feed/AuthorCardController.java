package com.bambi.service.feed;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.feed.dto.PublicCardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 작성자별 공개 카드 목록 — 프로필 화면의 브리핑 리스트(07-31, 메뉴 전면 연결).
 * 공개 프로필과 같은 정책으로 비로그인 열람 허용(SecurityConfig GET permitAll).
 * 전체 개수는 프로필 응답의 publicCardCount 를 쓴다(여기서 중복 계산하지 않음).
 */
@RestController
@RequestMapping("/api/users/{publicId}/cards")
public class AuthorCardController {

    private final FeedService feedService;

    public AuthorCardController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ApiResponse<List<PublicCardResponse>> authorCards(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String publicId,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Long viewerId = principal != null ? principal.id() : null;
        return ApiResponse.ok(feedService.publicCardsByAuthor(viewerId, publicId, limit));
    }
}
