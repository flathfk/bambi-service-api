package com.bambi.service.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로필 편집(PUT /api/users/me) — 목업 profile-self.html 편집 모달의 3필드.
 * username(핸들) 형식·유일성 검증은 정규화(trim·소문자) 이후여야 해서 서비스에서 한다.
 * 사진 업로드는 P2(이니셜 아바타로 시작).
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 30) String username,
        @Size(max = 120) String bio) {
}
