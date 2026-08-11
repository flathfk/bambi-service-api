package com.bambi.service.onboarding.dto;

/** 온보딩 완료 후 접수된 리포트 하나의 펜딩 정보. */
public record OnboardingReportResponse(
        int slot,
        String topic,
        String pendingId,
        String agentJobId) {
}
