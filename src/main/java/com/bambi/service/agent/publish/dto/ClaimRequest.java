package com.bambi.service.agent.publish.dto;

/**
 * 발행 배치 Claim 요청 (§4).
 * POST /internal/v1/publish-snapshot-batches/claim
 */
public record ClaimRequest(
        String workerId,
        int limit,
        int leaseSeconds) {
}
