package com.bambi.service.agent.publish.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 발행 배치 ACK 요청 (§4). 처리 끝난 항목만 담아 부분 성공 ACK.
 * POST /internal/v1/publish-snapshot-batches/{batchId}/ack
 * agent PublishBatchAckRequest 와 1:1 (snake_case).
 */
public record AckRequest(
        @JsonProperty("worker_id") String workerId,
        @JsonProperty("items") List<AckItem> items) {

    /** agent 가 failureReason 을 2000자로 제한한다. */
    private static final int MAX_FAILURE_REASON = 2000;

    /**
     * status: published | failed.
     * version / snapshotHash: Claim 응답 값 그대로 (불일치 시 409 PUBLISH_SNAPSHOT_MISMATCH).
     * retryable: 실패 시 필수 — true 면 Backoff 후 ready 복귀, false 면 최종 failed.
     * failureReason: 실패 시 필수. 비밀정보를 담지 않는다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)   // published 항목에 retryable/failure_reason 을 싣지 않는다
    public record AckItem(
            @JsonProperty("content_id") String contentId,
            @JsonProperty("version") Integer version,
            @JsonProperty("snapshot_hash") String snapshotHash,
            @JsonProperty("status") String status,
            @JsonProperty("retryable") Boolean retryable,
            @JsonProperty("failure_reason") String failureReason) {

        public static AckItem published(String contentId, Integer version, String snapshotHash) {
            return new AckItem(contentId, version, snapshotHash, "published", null, null);
        }

        public static AckItem failed(String contentId, Integer version, String snapshotHash,
                                     boolean retryable, String failureReason) {
            return new AckItem(contentId, version, snapshotHash, "failed",
                    retryable, truncate(failureReason));
        }

        /** agent 가 실패 ACK 에 failure_reason 을 요구하므로 빈 값이면 대체 문구를 넣는다. */
        private static String truncate(String reason) {
            if (reason == null || reason.isBlank()) {
                return "알 수 없는 발행 실패";
            }
            return reason.length() <= MAX_FAILURE_REASON
                    ? reason
                    : reason.substring(0, MAX_FAILURE_REASON);
        }
    }
}
