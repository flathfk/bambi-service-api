package com.bambi.service.wiki;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Wiki 빌드 작업의 멱등 등록과 상태 전이를 담당한다. */
public interface WikiBuildOperationRepository extends JpaRepository<WikiBuildOperation, UUID> {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = """
            INSERT INTO service.wiki_build_operations (
                id, user_id, source_event_id, root_agent_job_id, current_agent_job_id, status
            ) VALUES (:id, :userId, :sourceEventId, :jobId, :jobId, 'PENDING')
            ON CONFLICT (user_id, source_event_id) DO UPDATE SET
                root_agent_job_id = EXCLUDED.root_agent_job_id,
                current_agent_job_id = EXCLUDED.current_agent_job_id,
                status = CASE
                    WHEN service.wiki_build_operations.root_agent_job_id = EXCLUDED.root_agent_job_id
                    THEN service.wiki_build_operations.status
                    ELSE 'PENDING'
                END,
                error_code = CASE
                    WHEN service.wiki_build_operations.root_agent_job_id = EXCLUDED.root_agent_job_id
                    THEN service.wiki_build_operations.error_code
                    ELSE NULL
                END,
                updated_at = now(),
                completed_at = CASE
                    WHEN service.wiki_build_operations.root_agent_job_id = EXCLUDED.root_agent_job_id
                    THEN service.wiki_build_operations.completed_at
                    ELSE NULL
                END
            """, nativeQuery = true)
    void upsertAccepted(
            @Param("id") UUID id,
            @Param("userId") long userId,
            @Param("sourceEventId") String sourceEventId,
            @Param("jobId") String jobId);

    @Query(value = """
            SELECT * FROM service.wiki_build_operations
            WHERE status IN ('PENDING', 'RUNNING')
            ORDER BY created_at
            LIMIT :limit
            """, nativeQuery = true)
    List<WikiBuildOperation> findPollable(@Param("limit") int limit);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE service.wiki_build_operations
            SET status = :status, error_code = :errorCode, updated_at = now(),
                completed_at = CASE WHEN :status IN ('COMPLETED', 'FAILED', 'CANCELLED') THEN now() ELSE NULL END
            WHERE id = :id
            """, nativeQuery = true)
    void updateStatus(@Param("id") UUID id, @Param("status") String status,
                      @Param("errorCode") String errorCode);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE service.wiki_build_operations
            SET current_agent_job_id = :jobId, status = 'PENDING', error_code = NULL, updated_at = now()
            WHERE id = :id
            """, nativeQuery = true)
    void advanceToWikiJob(@Param("id") UUID id, @Param("jobId") String jobId);

    long countByUserIdAndStatusIn(Long userId, List<String> statuses);

    Optional<WikiBuildOperation> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);
}
