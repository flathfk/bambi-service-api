package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogResponse;
import com.bambi.service.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 — AI 처리 로그 조회.
 *
 * /api/admin/** 는 SecurityConfig 에서 ADMIN 권한으로 막혀 있어 별도 권한 체크는 두지 않는다.
 */
@RestController
@RequestMapping("/api/admin/ai-logs")
public class AdminAiLogController {

    private final AdminAiLogService adminAiLogService;

    public AdminAiLogController(AdminAiLogService adminAiLogService) {
        this.adminAiLogService = adminAiLogService;
    }

    @GetMapping
    public ApiResponse<List<AdminAiLogResponse>> list() {
        return ApiResponse.ok(adminAiLogService.listLogs());
    }
}
