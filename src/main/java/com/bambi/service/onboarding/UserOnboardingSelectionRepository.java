package com.bambi.service.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

/** 사용자별 온보딩 관심 분류 선택 저장소. */
public interface UserOnboardingSelectionRepository
        extends JpaRepository<UserOnboardingSelection, Long> {
}
