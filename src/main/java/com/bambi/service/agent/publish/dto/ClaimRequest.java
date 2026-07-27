package com.bambi.service.agent.publish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 발행 배치 Claim 요청 (§4).
 * POST /internal/v1/publish-snapshot-batches/claim
 * agent PublishBatchClaimRequest 와 1:1 (snake_case).
 *
 * @param workerId     배치를 처리할 워커 인스턴스 식별자 (1~128자)
 * @param limit        한 번에 Claim 할 최대 Snapshot 수 (1~100)
 * @param leaseSeconds 다른 워커의 중복 Claim 을 막는 lease 시간 (30~600초)
 */
public record ClaimRequest(
        @JsonProperty("worker_id") String workerId,
        @JsonProperty("limit") int limit,
        @JsonProperty("lease_seconds") int leaseSeconds) {
}
