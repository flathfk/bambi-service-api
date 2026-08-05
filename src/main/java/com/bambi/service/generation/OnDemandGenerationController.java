package com.bambi.service.generation;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.generation.dto.GenerationTriggerRequest;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 즉시 리포트 생성 트리거 — 사용자가 "지금 생성"을 눌러 스케줄러를 기다리지 않고 바로 요청한다.
 * 인증 사용자 본인 대상이며, 실제 생성은 {@link OnDemandGenerationService} 가 {@link GenerationClient} 로 위임한다.
 */
@RestController
@RequestMapping("/api/reports")
public class OnDemandGenerationController {

    private final OnDemandGenerationService onDemandGenerationService;

    public OnDemandGenerationController(OnDemandGenerationService onDemandGenerationService) {
        this.onDemandGenerationService = onDemandGenerationService;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerationTriggerResponse> generate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody(required = false) GenerationTriggerRequest request) {
        String topic = request != null ? request.topic() : null;
        return ApiResponse.ok(onDemandGenerationService.generateForUser(principal.id(), topic));
    }
}
