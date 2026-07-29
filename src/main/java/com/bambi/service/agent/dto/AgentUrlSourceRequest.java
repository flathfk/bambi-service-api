package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * agent-api URL 원천 처리 요청 바디.
 * 계약: POST /internal/v1/users/{userId}/wiki-sources/urls (202 + job_id).
 * agent {@code UrlWikiSourceRequest}(snake_case)와 1:1.
 *
 * <p>본문 없이 URL만 저장한 북마크를 개인 Wiki 처리로 넘길 때 쓴다(본문이 있으면
 * {@link AgentClippingRequest} 로 clippings 엔드포인트를 쓴다).
 *
 * @param sourceEventId 멱등 처리용 원천 이벤트 ID (예: {@code bookmark-{id}})
 * @param url           개인 Wiki에 반영할 URL
 * @param memo          사용자 메모(선택)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentUrlSourceRequest(
        @JsonProperty("source_event_id") String sourceEventId,
        @JsonProperty("url") String url,
        @JsonProperty("memo") String memo) {

    /** 멱등 키·URL 만으로 만드는 기본 생성자 (memo 없음). */
    public static AgentUrlSourceRequest of(String sourceEventId, String url) {
        return new AgentUrlSourceRequest(sourceEventId, url, null);
    }
}
