package com.bambi.service.follow.dto;

/**
 * 팔로우/언팔 결과 — 프론트 낙관적 업데이트의 확정값(현재 팔로우 상태 + 대상의 팔로워 수).
 */
public record FollowResponse(
        boolean following,
        long followerCount) {
}
