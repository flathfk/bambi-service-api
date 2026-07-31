package com.bambi.service.report;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.report.dto.ReportResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리포트(본문) 단건 조회 — 카드 상세의 본문 진입용.
 * 대외 식별자는 publicId(UUID). 권한은 카드 visibility 를 따른다(내 것 or PUBLIC 카드 참조).
 * 카드 요약은 GET /api/cards/{publicId}, 본문은 여기.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{publicId}")
    public ApiResponse<ReportResponse> get(@AuthenticationPrincipal AuthPrincipal principal,
                                           @PathVariable String publicId) {
        Long viewerId = principal != null ? principal.id() : null;
        return ApiResponse.ok(reportService.get(viewerId, publicId));
    }
}
