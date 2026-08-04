package com.bambi.service.user;

/**
 * 회원가입 도메인 이벤트. 커밋 전에는 Agent Context Outbox 적재,
 * 커밋 후에는 즉시 전달 시도를 auth 로직과 분리해 처리한다.
 */
public record UserRegisteredEvent(long userId) {
}
