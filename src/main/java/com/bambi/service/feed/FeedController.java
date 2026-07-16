package com.bambi.service.feed;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 카드 피드 (P0) — 로그인 사용자의 카드를 최신순으로 반환. 빈 배열이면 프론트가 Empty State 처리. */
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
}
