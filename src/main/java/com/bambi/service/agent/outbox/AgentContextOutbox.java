package com.bambi.service.agent.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service→Agent 사용자 컨텍스트 Transactional Outbox.
 *
 * <p>회원가입과 같은 트랜잭션에 PENDING 행을 만들고, HTTP 호출은 커밋 뒤 별도 처리한다.
 * PROCESSING lease가 만료되면 다른 워커가 다시 claim할 수 있어 프로세스 중단에도 유실되지 않는다.
 */
@Entity
@Table(name = "agent_context_outbox",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "context_version"}))
public class AgentContextOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "context_version", nullable = false)
    private int contextVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentContextOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "lock_token")
    private UUID lockToken;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_error", length = 100)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected AgentContextOutbox() {
    }

    public AgentContextOutbox(Long userId, int contextVersion, String payload, OffsetDateTime now) {
        this.userId = userId;
        this.contextVersion = contextVersion;
        this.payload = payload;
        this.status = AgentContextOutboxStatus.PENDING;
        this.nextAttemptAt = now;
    }

    /** 전송권을 lease 동안 획득하고 이번 시도 횟수를 올린다. */
    public UUID claim(String workerId, OffsetDateTime now, Duration leaseDuration) {
        this.status = AgentContextOutboxStatus.PROCESSING;
        this.attemptCount++;
        this.lockedBy = workerId;
        this.lockToken = UUID.randomUUID();
        this.lockedUntil = now.plus(leaseDuration);
        return this.lockToken;
    }

    /** 현재 claim 소유자만 완료 상태로 전환할 수 있는지 확인한다. */
    public boolean isClaimedBy(UUID token) {
        return status == AgentContextOutboxStatus.PROCESSING && lockToken != null && lockToken.equals(token);
    }

    /** Agent가 수신했거나 동일 버전을 이미 보유한 경우 전달 완료로 표시한다. */
    public void markPublished(OffsetDateTime now) {
        this.status = AgentContextOutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
        clearLease();
    }

    /** 실패한 전송을 다음 재시도 시각과 함께 대기 상태로 되돌린다. */
    public void scheduleRetry(OffsetDateTime now, Duration retryDelay, String failureCode) {
        this.status = AgentContextOutboxStatus.PENDING;
        this.nextAttemptAt = now.plus(retryDelay);
        this.lastError = failureCode;
        clearLease();
    }

    private void clearLease() {
        this.lockedBy = null;
        this.lockToken = null;
        this.lockedUntil = null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getContextVersion() {
        return contextVersion;
    }

    public String getPayload() {
        return payload;
    }

    public AgentContextOutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public UUID getLockToken() {
        return lockToken;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
