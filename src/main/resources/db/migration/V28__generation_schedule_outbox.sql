-- 03시 브리핑 준비와 07시 생성 요청을 외부 호출과 분리하는 스케줄 Outbox.
CREATE TABLE service.generation_schedule_runs (
    id BIGSERIAL PRIMARY KEY,
    phase VARCHAR(40) NOT NULL
        CHECK (phase IN ('BRIEFING_PREPARATION', 'MORNING_GENERATION')),
    schedule_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHING'
        CHECK (status IN ('PUBLISHING', 'PUBLISHED')),
    user_count INTEGER NOT NULL DEFAULT 0 CHECK (user_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    UNIQUE (phase, schedule_date)
);

CREATE TABLE service.generation_schedule_outbox (
    id BIGSERIAL PRIMARY KEY,
    phase VARCHAR(40) NOT NULL
        CHECK (phase IN ('BRIEFING_PREPARATION', 'MORNING_GENERATION')),
    schedule_date DATE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_by VARCHAR(120),
    lease_expires_at TIMESTAMPTZ,
    last_error TEXT,
    agent_job_id VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at TIMESTAMPTZ,
    UNIQUE (phase, schedule_date, user_id)
);

CREATE INDEX idx_generation_schedule_outbox_claim
    ON service.generation_schedule_outbox(status, next_attempt_at, lease_expires_at, id)
    WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_generation_schedule_outbox_user
    ON service.generation_schedule_outbox(user_id, schedule_date DESC);
