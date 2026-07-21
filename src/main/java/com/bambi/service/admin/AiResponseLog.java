package com.bambi.service.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Agent → Service 응답 로그 (service.ai_response_logs).
 *
 * 요청 한 건에 대한 처리 결과. 요청이 있는데 응답 Row 가 아직 없으면 = 처리 중으로 본다.
 * request_id 로 {@link AiRequestLog} 와 이어지며, response_body(jsonb)는 화면에 안 써 매핑하지 않는다.
 */
@Entity
@Table(name = "ai_response_logs")
public class AiResponseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "status_code")
    private Integer statusCode; // HTTP 상태코드. 2xx=성공, 그 외=실패

    @Column(name = "latency_ms")
    private Integer latencyMs; // 처리 소요시간(ms)

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiResponseLog() {
    }

    public Long getRequestId() {
        return requestId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }
}
