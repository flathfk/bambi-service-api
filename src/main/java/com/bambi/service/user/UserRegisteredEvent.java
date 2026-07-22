package com.bambi.service.user;

/**
 * 회원가입 완료 도메인 이벤트. 가입 트랜잭션 커밋 후 소비된다.
 * (agent 컨텍스트 동기화 등 "가입 이후" 부수 작업을 auth 로직과 분리하기 위함)
 */
public record UserRegisteredEvent(long userId) {
}
