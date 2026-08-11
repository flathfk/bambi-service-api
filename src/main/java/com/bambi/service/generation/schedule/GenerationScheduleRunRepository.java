package com.bambi.service.generation.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/** 분산 스케줄러 잠금과 날짜·단계별 발행 완료 기록을 관리한다. */
public interface GenerationScheduleRunRepository extends JpaRepository<GenerationScheduleRun, Long> {

    /** 현재 DB 트랜잭션 동안만 유지되는 PostgreSQL 분산 잠금을 획득한다. */
    @Query(value = "SELECT pg_try_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    boolean tryAcquireLock(@Param("lockKey") String lockKey);

    boolean existsByPhaseAndScheduleDate(GenerationSchedulePhase phase, LocalDate scheduleDate);
}
