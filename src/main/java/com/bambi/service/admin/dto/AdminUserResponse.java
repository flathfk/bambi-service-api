package com.bambi.service.admin.dto;

import com.bambi.service.user.User;

import java.time.LocalDate;

/**
 * 관리자 사용자 목록의 한 줄.
 *
 * User 엔티티에서 관리자 화면에 실제로 필요한 필드만 추린다. admin-web 목업이 기대하던
 * plan·최근활동·공개브리핑은 아직 users 스키마에 없어서 여기 담지 않는다(P1, 우석/영현과 조율).
 * 지금 내려줄 수 있는 것만 정직하게 내보내고, 화면(admin-web)도 이 필드에 맞춘다.
 */
public record AdminUserResponse(
        Long id,
        String displayName,
        String email,
        LocalDate joinedAt, // 가입일 — createdAt 의 날짜 부분 (YYYY-MM-DD 로 직렬화)
        String status // ACTIVE | INACTIVE — soft delete 여부로 파생
) {

    public static AdminUserResponse from(User user) {
        // deletedAt 이 찍혀 있으면 탈퇴/비활성 계정으로 본다.
        String status = user.getDeletedAt() == null ? "ACTIVE" : "INACTIVE";
        return new AdminUserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getCreatedAt().toLocalDate(),
                status);
    }
}
