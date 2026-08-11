package com.bambi.service.generation.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 스케줄 시각과 실제 Agent 호출 사이를 분리하는 사용자별 Outbox 이벤트. */
@Entity
@Table(name = "generation_schedule_outbox")
public class GenerationScheduleOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private GenerationSchedulePhase phase;

    @Column(name = "schedule_date", nullable = false, updatable = false)
    private LocalDate scheduleDate;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "lease_expires_at")
    private OffsetDateTime leaseExpiresAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "agent_job_id", length = 200)
    private String agentJobId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    protected GenerationScheduleOutboxEvent() {
    }

    private GenerationScheduleOutboxEvent(
            GenerationSchedulePhase phase,
            LocalDate scheduleDate,
            Long userId) {
        this.phase = phase;
        this.scheduleDate = scheduleDate;
        this.userId = userId;
        this.status = "PENDING";
        this.attemptCount = 0;
        this.nextAttemptAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    /** 사용자 한 명의 지정일 스케줄 작업을 만든다. */
    public static GenerationScheduleOutboxEvent pending(
            GenerationSchedulePhase phase,
            LocalDate scheduleDate,
            long userId) {
        return new GenerationScheduleOutboxEvent(phase, scheduleDate, userId);
    }

    /** 폴러가 지정 시간 동안 이벤트 처리 권한을 점유한다. */
    public void markProcessing(String workerId, int leaseSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = "PROCESSING";
        this.attemptCount += 1;
        this.lockedBy = workerId;
        this.leaseExpiresAt = now.plusSeconds(leaseSeconds);
        this.updatedAt = now;
    }

    /** 현재 PROCESSING 이벤트를 점유한 Worker인지 확인한다. */
    public boolean isOwnedBy(String workerId) {
        return "PROCESSING".equals(status) && Objects.equals(lockedBy, workerId);
    }

    /** Agent 접수 또는 생성 불필요 판단이 끝난 이벤트를 종결한다. */
    public void markDelivered(String agentJobId) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = "DELIVERED";
        this.agentJobId = agentJobId;
        this.lastError = null;
        this.lockedBy = null;
        this.leaseExpiresAt = null;
        this.deliveredAt = now;
        this.updatedAt = now;
    }

    /** 실패 이벤트를 지수 지연 후 재시도하거나 최대 시도에 도달하면 DEAD로 종결한다. */
    public void markRetry(
            Throwable error,
            int maxAttempts,
            int retryBaseSeconds,
            int retryMaxSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        this.lastError = safeError(error);
        this.lockedBy = null;
        this.leaseExpiresAt = null;
        this.updatedAt = now;
        if (attemptCount >= maxAttempts) {
            this.status = "DEAD";
            return;
        }
        long multiplier = 1L << Math.min(Math.max(attemptCount - 1, 0), 20);
        long delaySeconds = Math.min(retryMaxSeconds, retryBaseSeconds * multiplier);
        this.status = "PENDING";
        this.nextAttemptAt = now.plusSeconds(delaySeconds);
    }

    private String safeError(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "unknown error"
                : error.getMessage();
        return message.substring(0, Math.min(message.length(), 2000));
    }

    public Long getId() { return id; }
    public GenerationSchedulePhase getPhase() { return phase; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getLockedBy() { return lockedBy; }
    public OffsetDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public String getAgentJobId() { return agentJobId; }
}
