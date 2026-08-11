package com.bambi.service.generation;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.generation.dto.DevelopmentWikiInterestGenerationRequest;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 개발 서버에서 로그인한 본인의 실제 리포트 생성 경로를 즉시 검증하는 API. */
@RestController
@RequestMapping("/api/dev/reports/generate")
@ConditionalOnProperty(
        name = "app.development.report-triggers-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DevelopmentReportGenerationController {

    private final DevelopmentReportGenerationService generationService;

    public DevelopmentReportGenerationController(DevelopmentReportGenerationService generationService) {
        this.generationService = generationService;
    }

    /** 아침 브리핑을 실제 스케줄러와 같은 다중 주제 경로로 즉시 접수한다. */
    @PostMapping("/morning")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<GenerationTriggerResponse> morning(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(generationService.generateMorning(principal.id()));
    }

    /** 선택한 현재 활성 Wiki 관심사로 INTEREST_BUNDLE 리포트를 즉시 접수한다. */
    @PostMapping("/wiki-interest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<GenerationTriggerResponse> wikiInterest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody @Valid DevelopmentWikiInterestGenerationRequest request) {
        return ApiResponse.ok(generationService.generateWikiInterest(
                principal.id(), request.normalizedTagId()));
    }
}
