package com.bambi.service.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface GenerationPendingRepository extends JpaRepository<GenerationPending, UUID> {

    /**
     * 접수 멱등 insert — 같은 id(멱등키 파생)·같은 멱등키 재접수는 조용히 흡수한다
     * (같은 분 연타·스케줄러 재시도가 행을 늘리지 않게). NotificationRepository 와 같은 패턴.
     * 호출부(트리거)는 트랜잭션 없는 HTTP 경로라 REQUIRES_NEW 로 여기서 직접 연다
     * (서비스 내부 self-invocation 은 프록시를 안 타 @Transactional 이 무시된다).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            INSERT INTO service.generation_pendings (
                id, user_id, idempotency_key, report_type, topic, content_type, agent_job_id, status
            ) VALUES (
                :id, :userId, :idempotencyKey, :reportType, :topic, :contentType, :agentJobId, 'PENDING'
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertPending(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("reportType") String reportType,
            @Param("topic") String topic,
            @Param("contentType") String contentType,
            @Param("agentJobId") String agentJobId);

    List<GenerationPending> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId, OffsetDateTime after);

    List<GenerationPending> findByUserIdAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId, List<String> statuses, OffsetDateTime after);

    @Query(value = """
            SELECT * FROM service.generation_pendings
            WHERE status IN ('PENDING', 'RUNNING')
              AND agent_job_id IS NOT NULL
            ORDER BY created_at
            LIMIT :limit
            """, nativeQuery = true)
    List<GenerationPending> findPollable(@Param("limit") int limit);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE service.generation_pendings
            SET status = :status, error_code = :errorCode, updated_at = now(),
                completed_at = CASE WHEN :status IN ('FAILED', 'CANCELLED') THEN now() ELSE NULL END
            WHERE id = :id
            """, nativeQuery = true)
    void updateStatus(@Param("id") UUID id, @Param("status") String status,
                      @Param("errorCode") String errorCode);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE service.generation_pendings
            SET status = 'COMPLETED', error_code = NULL, updated_at = now(), completed_at = now()
            WHERE user_id = :userId AND idempotency_key = :idempotencyKey
              AND status <> 'COMPLETED'
            """, nativeQuery = true)
    int markCompleted(@Param("userId") Long userId,
                      @Param("idempotencyKey") String idempotencyKey);
}
