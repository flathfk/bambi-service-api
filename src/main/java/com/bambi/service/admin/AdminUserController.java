package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminUserResponse;
import com.bambi.service.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 — 사용자 목록 조회.
 *
 * /api/admin/** 는 SecurityConfig 에서 ADMIN 권한으로 막혀 있어, 여기 도달했다는 건
 * 이미 관리자 인증이 끝났다는 뜻이다. 그래서 컨트롤러는 별도 권한 체크 없이 조회만 위임한다.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserService.listUsers());
    }
}
