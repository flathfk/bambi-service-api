package com.bambi.service.agent.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentContextOutboxRepository extends JpaRepository<AgentContextOutbox, Long> {

    /** due 행과 만료된 lease를 여러 인스턴스가 겹치지 않게 claim한다. */
    @Query(value = """
            SELECT *
              FROM service.agent_context_outbox
             WHERE (status = 'PENDING' AND next_attempt_at <= :now)
                OR (status = 'PROCESSING' AND locked_until <= :now)
             ORDER BY next_attempt_at, id
             LIMIT :batchSize
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AgentContextOutbox> lockClaimableBatch(@Param("now") OffsetDateTime now,
                                                @Param("batchSize") int batchSize);

    /** 가입 커밋 직후 해당 사용자의 방금 적재된 행을 즉시 claim한다. */
    @Query(value = """
            SELECT *
              FROM service.agent_context_outbox
             WHERE user_id = :userId
               AND ((status = 'PENDING' AND next_attempt_at <= :now)
                 OR (status = 'PROCESSING' AND locked_until <= :now))
             ORDER BY context_version
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AgentContextOutbox> lockClaimableForUser(@Param("userId") long userId,
                                                  @Param("now") OffsetDateTime now);

    /** 완료/실패 갱신이 재-claim과 경합하지 않도록 대상 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AgentContextOutbox o where o.id = :id")
    Optional<AgentContextOutbox> findByIdForUpdate(@Param("id") long id);
}
