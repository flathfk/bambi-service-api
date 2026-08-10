package com.bambi.service.wiki;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Service가 추적하는 사용자 Wiki 빌드 작업. */
@Entity
@Table(name = "wiki_build_operations")
public class WikiBuildOperation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;

    @Column(name = "root_agent_job_id", nullable = false)
    private String rootAgentJobId;

    @Column(name = "current_agent_job_id", nullable = false)
    private String currentAgentJobId;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected WikiBuildOperation() {
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSourceEventId() { return sourceEventId; }
    public String getCurrentAgentJobId() { return currentAgentJobId; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
