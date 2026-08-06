-- =============================================================
-- V14__generation_pendings.sql — 생성 접수(펜딩) 영속화 (2026-08-06 합의, 우석)
-- -------------------------------------------------------------
-- 온디맨드/스케줄러가 agent 에 생성을 접수한 사실을 남겨 프론트 "처리중" 슬롯의
-- 근거를 만든다. id 는 service 가 멱등키에서 파생한 결정적 UUID (같은 접수 = 같은 행).
-- report_type: MORNING_BRIEFING(스케줄러) | ON_DEMAND(즉시 생성) — 접수 시점에 service 가
--   직접 알 수 있는 값이라 claim 을 기다리지 않는다.
-- status: PENDING 만 사용 (MVP). 완료 전환은 claim 계약에 생성요청 연결고리가 붙은 뒤
--   후속 과제(소라 협의) — 프론트는 최근 60분 PENDING 만 노출한다.
-- =============================================================

CREATE TABLE service.generation_pendings (
    id              UUID PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(200) NOT NULL,
    report_type     VARCHAR(30),
    topic           VARCHAR(500),
    content_type    VARCHAR(100),
    agent_job_id    VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (idempotency_key)
);

CREATE INDEX idx_generation_pendings_user_status_created
    ON service.generation_pendings(user_id, status, created_at DESC);
