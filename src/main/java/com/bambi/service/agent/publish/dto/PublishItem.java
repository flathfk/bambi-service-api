package com.bambi.service.agent.publish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 발행 스냅샷 배치의 한 항목 (docs/service-integration-guide.md §4 Claim 응답).
 * Claim 응답에 전체 Payload 가 담겨 추가 조회 없이 바로 Upsert 할 수 있다.
 * agent PublishSnapshotResponse 와 1:1 (snake_case).
 *
 * <p>userId 는 agent 계약상 문자열이다(agent 는 사용자 ID 를 불투명 식별자로 다룬다).
 * service-db 의 users.id 는 Long 이므로 {@link #userIdAsLong()} 로 변환해서 쓴다.
 * agent 가 UUID 등 숫자가 아닌 ID 를 쓰기로 바뀌면 이 메서드만 고치면 된다.
 */
public record PublishItem(
        @JsonProperty("content_id") String contentId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("version") Integer version,
        @JsonProperty("snapshot_hash") String snapshotHash,
        @JsonProperty("title") String title,
        @JsonProperty("summary") String summary,
        @JsonProperty("body") String body,
        @JsonProperty("citations") List<Citation> citations) {

    /**
     * service-db 저장용 사용자 ID. 숫자가 아니면 처리 실패로 보고 예외를 던진다
     * (워커가 retryable=false 로 ACK 하도록).
     */
    public Long userIdAsLong() {
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "agent user_id 가 service-db 사용자 ID 형식이 아님: " + userId, e);
        }
    }

    /** 출처(인용). 출처 없는 카드 금지 원칙의 근거 데이터. */
    public record Citation(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url) {
    }
}
