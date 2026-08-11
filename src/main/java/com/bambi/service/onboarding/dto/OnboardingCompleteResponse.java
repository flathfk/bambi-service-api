package com.bambi.service.onboarding.dto;

import java.util.List;

/** 온보딩 완료 처리로 접수된 최대 3개의 리포트. */
public record OnboardingCompleteResponse(List<OnboardingReportResponse> reports) {

    public OnboardingCompleteResponse {
        reports = List.copyOf(reports);
    }
}
