-- 사용자에게 LLM Wiki 빌드 상태를 제공하기 위한 Agent Job 추적 행.
CREATE TABLE service.wiki_build_operations (
    id                   UUID PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    source_event_id      VARCHAR(200) NOT NULL,
    root_agent_job_id    VARCHAR(200) NOT NULL,
    current_agent_job_id VARCHAR(200) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_code           VARCHAR(100),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at         TIMESTAMPTZ,
    UNIQUE (user_id, source_event_id)
);

CREATE INDEX idx_wiki_build_operations_poll
    ON service.wiki_build_operations(status, created_at)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE INDEX idx_wiki_build_operations_user_updated
    ON service.wiki_build_operations(user_id, updated_at DESC);
