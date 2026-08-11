package com.bambi.service.generation.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

/** 아침 브리핑 스케줄 Outbox 이벤트 저장소. */
public interface GenerationScheduleOutboxRepository
        extends JpaRepository<GenerationScheduleOutboxEvent, Long> {
}
