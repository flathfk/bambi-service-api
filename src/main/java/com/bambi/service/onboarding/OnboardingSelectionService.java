package com.bambi.service.onboarding;

import com.bambi.service.onboarding.dto.OnboardingSelectionRequest;
import com.bambi.service.onboarding.dto.OnboardingSelectionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 온보딩 관심 분류 선택 저장과 Agent Context 재동기화를 조율한다. */
@Service
public class OnboardingSelectionService {

    private final UserOnboardingSelectionRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final String interestTaxonomyVersion;

    public OnboardingSelectionService(
            UserOnboardingSelectionRepository repository,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.onboarding.interest-taxonomy-version:1.0.0-draft}")
            String interestTaxonomyVersion) {
        if (interestTaxonomyVersion == null || interestTaxonomyVersion.isBlank()
                || interestTaxonomyVersion.length() > 50) {
            throw new IllegalArgumentException("관심사 분류체계 버전은 1~50자여야 합니다.");
        }
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.interestTaxonomyVersion = interestTaxonomyVersion;
    }

    /** 선택 전체를 저장하고 같은 트랜잭션에서 Agent Context Outbox 적재를 요청한다. */
    @Transactional
    public OnboardingSelectionResponse replace(long userId, OnboardingSelectionRequest request) {
        List<String> categoryIds = normalize(request.selectedCategoryIds());
        List<String> topicIds = normalize(request.selectedTopicIds());
        UserOnboardingSelection selection = repository.findById(userId)
                .orElseGet(() -> new UserOnboardingSelection(
                        userId, interestTaxonomyVersion, categoryIds, topicIds));
        selection.replace(interestTaxonomyVersion, categoryIds, topicIds);
        repository.save(selection);
        eventPublisher.publishEvent(new OnboardingSelectionsChangedEvent(userId));
        return OnboardingSelectionResponse.from(selection);
    }

    /** 저장된 선택을 반환하며 아직 온보딩하지 않았다면 빈 선택을 반환한다. */
    @Transactional(readOnly = true)
    public OnboardingSelectionResponse get(long userId) {
        return repository.findById(userId)
                .map(OnboardingSelectionResponse::from)
                .orElseGet(() -> OnboardingSelectionResponse.empty(interestTaxonomyVersion));
    }

    /** 앞뒤 공백과 중복을 제거하되 최초 선택 순서를 유지한다. */
    private static List<String> normalize(List<String> ids) {
        return ids.stream().map(String::strip).distinct().toList();
    }
}
