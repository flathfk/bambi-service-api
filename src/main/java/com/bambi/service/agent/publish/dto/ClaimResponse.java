package com.bambi.service.agent.publish.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 발행 배치 Claim 응답 (§4). 처리할 것이 없으면 items=[] (batchId=null).
 */
public record ClaimResponse(
        String batchId,
        OffsetDateTime leaseExpiresAt,
        List<PublishItem> items) {

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public static ClaimResponse empty() {
        return new ClaimResponse(null, null, List.of());
    }
}
