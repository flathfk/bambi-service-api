package com.bambi.service.feed;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.feed.dto.PublicCardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카드 피드.
 * - GET /api/feed         : 내 카드 피드 (P0)
 * - GET /api/feed/public  : 공개 피드 (SNS/Week2) — following=true 면 팔로잉 스코프
 * 빈 배열이면 프론트가 Empty State 를 처리한다.
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ApiResponse<List<CardResponse>> myFeed(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(feedService.myFeed(principal.id()));
    }

    @GetMapping("/public")
    public ApiResponse<List<PublicCardResponse>> publicFeed(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "following", defaultValue = "false") boolean following,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        // 비로그인 열람 허용 → 익명이면 principal 이 null. viewerId=null 로 넘긴다.
        // (익명이면 liked=false, 팔로잉 스코프는 서비스가 401 로 막는다)
        Long viewerId = principal != null ? principal.id() : null;
        return ApiResponse.ok(feedService.publicFeed(viewerId, following, limit));
    }
}
