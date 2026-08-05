package com.bambi.service.generation;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 즉시 리포트 생성 트리거 — 사용자가 "지금 생성"을 눌러 스케줄러를 기다리지 않고 바로 요청한다.
 * 온디맨드는 사용자 관심사 전체를 종합하는 보고서라 별도 body 없이 본인 관심사 기준으로 생성한다.
 * 실제 생성은 {@link OnDemandGenerationService} 가 {@link GenerationClient} 로 위임한다.
 */
@RestController
@RequestMapping("/api/reports")
public class OnDemandGenerationController {

    private final OnDemandGenerationService onDemandGenerationService;

    public OnDemandGenerationController(OnDemandGenerationService onDemandGenerationService) {
        this.onDemandGenerationService = onDemandGenerationService;
    }

    /** 비동기 접수라 202 Accepted. body 는 job_id 를 담아 펜딩 UI 가 상태를 추적하게 한다. */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<GenerationTriggerResponse> generate(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(onDemandGenerationService.generateForUser(principal.id()));
    }
}
