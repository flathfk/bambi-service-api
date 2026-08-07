package com.bambi.service.interest;

/**
 * 사용자 관심사가 추가·수정·삭제됐다는 도메인 이벤트.
 * 변경 트랜잭션 커밋 후 소비돼 agent 컨텍스트를 재동기화한다
 * (프론트의 명시적 {@code POST /api/interests/sync} 호출에 의존하지 않게 하는 안전망).
 */
public record InterestChangedEvent(long userId) {
}
