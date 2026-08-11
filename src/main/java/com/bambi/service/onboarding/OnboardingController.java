package com.bambi.service.onboarding;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.onboarding.dto.OnboardingCompleteRequest;
import com.bambi.service.onboarding.dto.OnboardingCompleteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입 온보딩 완료와 첫 리포트 생성을 연결하는 인증 API. */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingCompletionService completionService;

    public OnboardingController(OnboardingCompletionService completionService) {
        this.completionService = completionService;
    }

    /** 관심사 선택 순서를 확정하고 최대 3개의 비동기 리포트 생성을 접수한다. */
    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<OnboardingCompleteResponse> complete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody OnboardingCompleteRequest request) {
        return ApiResponse.ok(completionService.complete(principal.id(), request));
    }
}
