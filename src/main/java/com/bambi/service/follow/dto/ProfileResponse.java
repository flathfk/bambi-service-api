package com.bambi.service.follow.dto;

import java.util.UUID;

/**
 * 공개 프로필 — 팔로우 버튼/프로필 헤더용. 순번 id 대신 publicId 로만 사용자를 노출한다.
 * following = "지금 보고 있는 나"가 이 사용자를 팔로우 중인지.
 */
public record ProfileResponse(
        UUID publicId,
        String username,
        String displayName,
        long followerCount,
        long followingCount,
        boolean following,
        long publicCardCount) {
}
