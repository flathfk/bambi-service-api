package com.bambi.service.onboarding.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 온보딩에서 사용자가 관심사를 선택한 순서. */
public record OnboardingCompleteRequest(
        @NotEmpty List<@NotNull Long> orderedInterestIds) {
}
