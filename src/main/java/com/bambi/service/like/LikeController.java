package com.bambi.service.like;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.like.dto.LikeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카드 좋아요 (SNS/Week2). 인증 필수, 대상 카드는 publicId(UUID)로 가리킨다.
 */
@RestController
@RequestMapping("/api/cards/{publicId}/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ApiResponse<LikeResponse> like(@AuthenticationPrincipal AuthPrincipal principal,
                                          @PathVariable String publicId) {
        return ApiResponse.ok(likeService.like(principal.id(), publicId));
    }

    @DeleteMapping
    public ApiResponse<LikeResponse> unlike(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PathVariable String publicId) {
        return ApiResponse.ok(likeService.unlike(principal.id(), publicId));
    }
}
