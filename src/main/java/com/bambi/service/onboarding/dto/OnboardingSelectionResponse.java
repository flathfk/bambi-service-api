package com.bambi.service.onboarding.dto;

import com.bambi.service.onboarding.UserOnboardingSelection;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 저장된 온보딩 Category·Topic 선택 응답. */
public record OnboardingSelectionResponse(
        @JsonProperty("interest_taxonomy_version") String interestTaxonomyVersion,
        @JsonProperty("selected_category_ids") List<String> selectedCategoryIds,
        @JsonProperty("selected_topic_ids") List<String> selectedTopicIds) {

    /** 영속 선택을 API 응답으로 변환한다. */
    public static OnboardingSelectionResponse from(UserOnboardingSelection selection) {
        return new OnboardingSelectionResponse(
                selection.getInterestTaxonomyVersion(),
                selection.getSelectedCategoryIds(),
                selection.getSelectedTopicIds());
    }

    /** 아직 선택하지 않은 사용자의 현재 분류체계 기준 빈 응답을 만든다. */
    public static OnboardingSelectionResponse empty(String taxonomyVersion) {
        return new OnboardingSelectionResponse(taxonomyVersion, List.of(), List.of());
    }
}
