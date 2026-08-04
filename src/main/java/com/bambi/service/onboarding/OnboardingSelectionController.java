package com.bambi.service.onboarding;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.onboarding.dto.OnboardingSelectionRequest;
import com.bambi.service.onboarding.dto.OnboardingSelectionResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 현재 사용자의 온보딩 관심 Category·Topic 선택 API. */
@RestController
@RequestMapping("/api/onboarding/interests")
public class OnboardingSelectionController {

    private final OnboardingSelectionService service;

    public OnboardingSelectionController(OnboardingSelectionService service) {
        this.service = service;
    }

    /** 현재 사용자의 온보딩 관심 분류 선택 전체를 교체한다. */
    @PutMapping
    public ApiResponse<OnboardingSelectionResponse> replace(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody OnboardingSelectionRequest request) {
        return ApiResponse.ok(service.replace(principal.id(), request));
    }

    /** 현재 사용자의 저장된 온보딩 관심 분류 선택을 조회한다. */
    @GetMapping
    public ApiResponse<OnboardingSelectionResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.get(principal.id()));
    }
}
