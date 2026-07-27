package com.bambi.service.agent.publish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 발행 배치 Claim 응답 (§4). 처리할 것이 없으면 items=[] (batchId=null).
 * agent PublishBatchClaimResponse 와 1:1 (snake_case).
 */
public record ClaimResponse(
        @JsonProperty("batch_id") String batchId,
        @JsonProperty("lease_expires_at") OffsetDateTime leaseExpiresAt,
        @JsonProperty("items") List<PublishItem> items) {

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public static ClaimResponse empty() {
        return new ClaimResponse(null, null, List.of());
    }
}
