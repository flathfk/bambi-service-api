package com.bambi.service.admin.dto;

/**
 * 관리자 사용자 활성/비활성 토글 요청 (PATCH /api/admin/users/{userId}/status).
 * active=true 면 활성화, false 면 비활성화.
 */
public record AdminUserStatusRequest(boolean active) {
}
