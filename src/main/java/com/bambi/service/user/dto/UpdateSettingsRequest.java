package com.bambi.service.user.dto;

/**
 * 사용자 설정 변경(PATCH /api/users/me/settings) — 부분 업데이트.
 * 두 필드 모두 선택적(null = 미변경). defaultCardVisibility 값 검증(PRIVATE/PUBLIC)은 서비스에서 한다.
 */
public record UpdateSettingsRequest(
        String defaultCardVisibility,
        Boolean reportReadyNotification) {
}
