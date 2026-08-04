-- =============================================================
-- V9__agent_context_outbox.sql — Agent 사용자 컨텍스트 전달 보장
-- -------------------------------------------------------------
-- 회원가입 트랜잭션 안에서 전송할 payload를 먼저 적재한다.
-- 커밋 뒤 HTTP 전송에 실패하거나 프로세스가 중단돼도 due/lease 인덱스로
-- 다시 claim하여 같은 context_version과 payload를 at-least-once 전송한다.
-- =============================================================

CREATE TABLE service.agent_context_outbox (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    context_version     INT NOT NULL CHECK (context_version > 0),
    payload             JSONB NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED')),
    attempt_count       INT NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_by           VARCHAR(100),
    lock_token          UUID,
    locked_until        TIMESTAMPTZ,
    last_error          VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at        TIMESTAMPTZ,
    UNIQUE (user_id, context_version),
    CHECK (
        (status = 'PROCESSING'
            AND locked_by IS NOT NULL AND lock_token IS NOT NULL AND locked_until IS NOT NULL)
        OR
        (status <> 'PROCESSING'
            AND locked_by IS NULL AND lock_token IS NULL AND locked_until IS NULL)
    ),
    CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR
        (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX ix_agent_context_outbox_due
    ON service.agent_context_outbox(next_attempt_at, id)
    WHERE status = 'PENDING';

CREATE INDEX ix_agent_context_outbox_expired_lease
    ON service.agent_context_outbox(locked_until, id)
    WHERE status = 'PROCESSING';

COMMENT ON TABLE service.agent_context_outbox IS
    'service→agent 사용자 컨텍스트 at-least-once 전달용 Transactional Outbox';
