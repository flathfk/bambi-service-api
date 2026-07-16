package com.bambi.service.bookmark;

/**
 * 북마크 처리 상태 — DB CHECK 제약(V1)과 동일한 값만 갖는다.
 * PROCESSING: Agent 처리 대기/중 · DONE: 카드까지 생성 완료 · FAILED: Agent 처리 실패
 */
public enum BookmarkStatus {
    PROCESSING, DONE, FAILED
}
