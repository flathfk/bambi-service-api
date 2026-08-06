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

    /** 본인 것만, 지정 시각 이후 접수된 PENDING 을 최신순으로 — 처리중 슬롯 노출용. */
    List<GenerationPending> findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId, String status, OffsetDateTime after);
}
