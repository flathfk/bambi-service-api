package com.bambi.service.agent.publish.dto;

import java.util.List;

/**
 * 발행 스냅샷 배치의 한 항목 (docs/service-integration-guide.md §4 Claim 응답).
 * Claim 응답에 전체 Payload 가 담겨 추가 조회 없이 바로 Upsert 할 수 있다.
 */
public record PublishItem(
        String contentId,
        Long userId,
        Integer version,
        String snapshotHash,
        String title,
        String summary,
        String body,
        List<Citation> citations) {

    /** 출처(인용). 출처 없는 카드 금지 원칙의 근거 데이터. */
    public record Citation(String title, String url) {
    }
}
