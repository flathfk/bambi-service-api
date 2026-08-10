package com.bambi.service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경(POST /api/auth/password). confirm 은 프론트 검증.
 * newPassword 정책은 가입과 동일(8~100자) — @Size 위반은 400(VALIDATION_ERROR).
 * 현재 비밀번호 불일치는 서비스에서 401(AUTH_INVALID_CREDENTIALS).
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword) {
}
