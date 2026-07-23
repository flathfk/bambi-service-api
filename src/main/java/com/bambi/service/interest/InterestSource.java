package com.bambi.service.interest;

/**
 * 관심사 출처 — DB CHECK 제약(V1)과 동일한 값만 갖는다.
 * USER: 사용자가 직접 입력 · INFERRED: agent 가 추론(P1, 이 API 로는 생성 안 함).
 */
public enum InterestSource {
    USER, INFERRED
}
