package com.bambi.service.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * agent-api 웹 클리핑 처리 요청 바디.
 * 계약: POST /internal/v1/users/{userId}/wiki-sources/clippings (202 + job_id).
 * agent {@code WebClippingRequest}(snake_case)와 1:1.
 *
 * <p>agent 필수 필드는 {@code source_event_id}·{@code source}(URL)·{@code title}·{@code content} 넷이다.
 * 특히 {@code content}(마크다운 본문)가 필수라, URL만 있고 본문이 없는 저장은 이 엔드포인트가 아니라
 * URL 원천(/wiki-sources/urls)으로 보내야 한다(중계 라우팅은 호출부 책임).
 *
 * @param sourceEventId 멱등 처리용 원천 이벤트 ID (예: {@code bookmark-{id}})
 * @param source        클리핑 원문 URL
 * @param title         클리핑 제목
 * @param content       마크다운 클리핑 본문
 * @param memo          사용자 메모(선택)
 * @param tags          클리퍼 태그 목록(없으면 빈 배열)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentClippingRequest(
        @JsonProperty("source_event_id") String sourceEventId,
        @JsonProperty("source") String source,
        @JsonProperty("title") String title,
        @JsonProperty("content") String content,
        @JsonProperty("memo") String memo,
        @JsonProperty("tags") List<String> tags) {

    /** 멱등 키·URL·제목·본문만으로 만드는 기본 생성자 (memo 없음, tags 빈 배열). */
    public static AgentClippingRequest of(String sourceEventId, String source, String title, String content) {
        return new AgentClippingRequest(sourceEventId, source, title, content, null, List.of());
    }
}
