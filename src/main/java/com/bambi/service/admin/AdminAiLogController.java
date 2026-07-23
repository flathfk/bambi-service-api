package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogDetailResponse;
import com.bambi.service.admin.dto.AdminAiLogResponse;
import com.bambi.service.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** 로그 한 건 상세 — 요청·응답 본문 포함. 없으면 NOT_FOUND. */
    @GetMapping("/{id}")
    public ApiResponse<AdminAiLogDetailResponse> detail(@PathVariable long id) {
        return ApiResponse.ok(adminAiLogService.getLogDetail(id));
    }
}
