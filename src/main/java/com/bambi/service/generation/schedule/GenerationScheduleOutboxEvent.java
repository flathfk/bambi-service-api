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

    public Long getId() { return id; }
    public GenerationSchedulePhase getPhase() { return phase; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
}
