package com.bambi.service.admin;

import com.bambi.service.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Service → Agent 요청 로그 (service.ai_request_logs).
 *
 * 관리자 화면에서 읽기 전용으로만 쓰므로, 조회에 필요한 컬럼만 매핑한다.
 * request_body(jsonb)는 화면에 안 쓰고 매핑 부담만 크므로 일부러 뺐다.
 * (적재는 실제 AgentGateway=P1 몫 — 지금 이 테이블은 비어 있다.)
 */
@Entity
@Table(name = "ai_request_logs")
public class AiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자의 요청이었는지. 이메일 표시를 위해 연결해 둔다(목록이라 LAZY).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String endpoint; // 호출한 agent 엔드포인트 (예: /agent/bookmarks/process)

    // DB default(now())가 채운다. 읽기 전용이라 삽입/수정 대상에서 뺀다.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiRequestLog() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
