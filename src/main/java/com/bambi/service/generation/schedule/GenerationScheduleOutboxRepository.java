package com.bambi.service.generation.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 아침 브리핑 스케줄 Outbox 이벤트 저장소. */
public interface GenerationScheduleOutboxRepository
        extends JpaRepository<GenerationScheduleOutboxEvent, Long> {

    /** 처리 가능 이벤트를 행 잠금으로 점유해 다른 Worker 인스턴스의 중복 Claim을 막는다. */
    @Query(value = """
            SELECT event.*
            FROM service.generation_schedule_outbox AS event
            WHERE (event.status = 'PENDING' AND event.next_attempt_at <= now())
               OR (event.status = 'PROCESSING'
                   AND (event.lease_expires_at IS NULL OR event.lease_expires_at <= now()))
            ORDER BY event.schedule_date, event.id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<GenerationScheduleOutboxEvent> findClaimable(@Param("limit") int limit);
}
