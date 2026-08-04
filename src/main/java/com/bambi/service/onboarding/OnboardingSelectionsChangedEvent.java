package com.bambi.service.onboarding;

/** 온보딩 관심 분류 선택 변경으로 Agent Context 재동기가 필요함을 알리는 이벤트. */
public record OnboardingSelectionsChangedEvent(long userId) {
}
