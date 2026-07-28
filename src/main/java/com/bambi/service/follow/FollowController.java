package com.bambi.service.follow;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.follow.dto.FollowResponse;
import com.bambi.service.follow.dto.ProfileResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팔로우/공개 프로필 (SNS/Week2). 모든 요청 인증 필수.
 * 대상 사용자는 publicId(UUID)로 가리킨다(순번 id 노출 방지 — Card 와 동일 컨벤션).
 */
@RestController
@RequestMapping("/api/users/{publicId}")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    public ApiResponse<FollowResponse> follow(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable String publicId) {
        return ApiResponse.ok(followService.follow(principal.id(), publicId));
    }

    @DeleteMapping("/follow")
    public ApiResponse<FollowResponse> unfollow(@AuthenticationPrincipal AuthPrincipal principal,
                                                @PathVariable String publicId) {
        return ApiResponse.ok(followService.unfollow(principal.id(), publicId));
    }

    /** 공개 프로필은 비로그인 열람 허용 → 익명이면 principal null(following=false). */
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal AuthPrincipal principal,
                                                @PathVariable String publicId) {
        Long viewerId = principal != null ? principal.id() : null;
        return ApiResponse.ok(followService.profile(viewerId, publicId));
    }
}
