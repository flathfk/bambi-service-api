package com.bambi.service.agent.publish.dto;

import java.util.List;

/**
 * 발행 배치 ACK 요청 (§4). 처리 끝난 항목만 담아 부분 성공 ACK.
 * POST /internal/v1/publish-snapshot-batches/{batchId}/ack
 */
public record AckRequest(
        String workerId,
        List<AckItem> items) {

    /**
     * status: published | failed.
     * retryable: 실패 시 필수 — true 면 Backoff 후 ready 복귀, false 면 최종 failed.
     * snapshotHash: Claim 응답 값 그대로 (불일치 시 409 PUBLISH_SNAPSHOT_MISMATCH).
     */
    public record AckItem(
            String contentId,
            String status,
            Boolean retryable,
            String snapshotHash) {

        public static AckItem published(String contentId, String snapshotHash) {
            return new AckItem(contentId, "published", null, snapshotHash);
        }

        public static AckItem failed(String contentId, String snapshotHash, boolean retryable) {
            return new AckItem(contentId, "failed", retryable, snapshotHash);
        }
    }
}
