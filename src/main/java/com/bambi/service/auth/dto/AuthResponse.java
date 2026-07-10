package com.bambi.service.auth.dto;

/** 로그인 응답: access token + 사용자 요약. (P0 는 localStorage 저장 전제) */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes,
        UserSummary user) {

    public static AuthResponse of(String token, long expiresInMinutes, UserSummary user) {
        return new AuthResponse(token, "Bearer", expiresInMinutes, user);
    }
}
