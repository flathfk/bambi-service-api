package com.bambi.service.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 온보딩 관심 분류 선택 전체 교체 요청. 빈 목록은 온보딩 건너뛰기를 뜻한다. */
public record OnboardingSelectionRequest(
        @NotNull @Size(max = 8) List<@Valid @NotBlank @Size(max = 100)
                @Pattern(regexp = "[a-z0-9_-]+") String> selectedCategoryIds,
        @NotNull @Size(max = 12) List<@Valid @NotBlank @Size(max = 100)
                @Pattern(regexp = "[a-z0-9_-]+") String> selectedTopicIds) {
}
