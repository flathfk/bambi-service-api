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

/** 날짜·단계별 Outbox 발행 완료 여부를 보존하는 배치 실행 기록. */
@Entity
@Table(name = "generation_schedule_runs")
public class GenerationScheduleRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private GenerationSchedulePhase phase;

    @Column(name = "schedule_date", nullable = false, updatable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "user_count", nullable = false)
    private int userCount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected GenerationScheduleRun() {
    }

    private GenerationScheduleRun(GenerationSchedulePhase phase, LocalDate scheduleDate) {
        this.phase = phase;
        this.scheduleDate = scheduleDate;
        this.status = "PUBLISHING";
        this.userCount = 0;
    }

    /** 새로운 날짜·단계 배치 실행 기록을 만든다. */
    public static GenerationScheduleRun start(
            GenerationSchedulePhase phase,
            LocalDate scheduleDate) {
        return new GenerationScheduleRun(phase, scheduleDate);
    }

    /** 모든 활성 사용자 이벤트가 기록된 실행을 완료한다. */
    public void markPublished(int userCount) {
        this.status = "PUBLISHED";
        this.userCount = userCount;
        this.completedAt = OffsetDateTime.now();
    }

    public String getStatus() { return status; }
    public int getUserCount() { return userCount; }
}
