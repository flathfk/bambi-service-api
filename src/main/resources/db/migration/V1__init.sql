-- =============================================================
-- V1__init.sql  —  밤새비서(Bambi) 초기 스키마 (P0 / MVP)
-- -------------------------------------------------------------
-- Flyway 규칙: 이 파일은 "첫 배포 전"까지만 수정 가능.
--             배포 후 변경은 반드시 V2__*.sql 새 파일로 추가한다.
--
-- 미래 대비(값싼 것만): 아래 4가지를 미리 심어둠 (SNS 확장 대비)
--   1) 모든 핵심 테이블에 created_at / updated_at / deleted_at(soft delete)
--   2) cards / feed_items 에 visibility (기본 PRIVATE)  → 공개 피드(P1) 스위치
--   3) users 에 공개용 username(unique)               → 공개 프로필(SNS)
--   4) 공개 리소스(users, cards)에 public_id UUID       → 순번 노출/크롤링 방지
--
-- 참고: 상태값은 ENUM 대신 VARCHAR + CHECK 사용.
--       (postgres ENUM은 값 추가/변경 시 마이그레이션이 아파서 확장에 불리)
-- =============================================================

-- ── 스키마 분리: service(원본·최종) / agent(AI 파생) ──
CREATE SCHEMA IF NOT EXISTS service;
CREATE SCHEMA IF NOT EXISTS agent;


-- =============================================================
-- service schema
-- =============================================================

-- 권한 (USER / ADMIN)
CREATE TABLE service.roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30) UNIQUE NOT NULL     -- 'USER', 'ADMIN'
);

-- 사용자
CREATE TABLE service.users (
    id            BIGSERIAL PRIMARY KEY,
    public_id     UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),  -- 대외 노출용 id
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username      VARCHAR(50)  UNIQUE,           -- 공개 프로필용(지금은 NULL 허용, SNS 때 필수화)
    display_name  VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ                    -- soft delete (NULL = 살아있음)
);

-- 사용자 ↔ 권한 (N:M)
CREATE TABLE service.user_roles (
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES service.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 북마크 (저장한 URL/본문)
CREATE TABLE service.bookmarks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    url         TEXT,
    title       VARCHAR(500),
    content     TEXT,
    summary     TEXT,                            -- Agent가 만든 요약을 Service가 저장
    status      VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
                CHECK (status IN ('PROCESSING', 'DONE', 'FAILED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_bookmarks_user   ON service.bookmarks(user_id);
CREATE INDEX idx_bookmarks_status ON service.bookmarks(status);
-- 중복 URL 처리: 같은 유저가 같은 url 두 번 저장 방지 (살아있는 것만)
CREATE UNIQUE INDEX uq_bookmarks_user_url
    ON service.bookmarks(user_id, url) WHERE deleted_at IS NULL AND url IS NOT NULL;

-- 관심사
CREATE TABLE service.interests (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    source      VARCHAR(20) NOT NULL DEFAULT 'USER'   -- 'USER'(직접입력) / 'INFERRED'(AI추론)
                CHECK (source IN ('USER', 'INFERRED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_interests_user_name
    ON service.interests(user_id, name) WHERE deleted_at IS NULL;

-- 북마크 ↔ 관심사 매칭 (N:M)
CREATE TABLE service.bookmark_interests (
    bookmark_id BIGINT NOT NULL REFERENCES service.bookmarks(id) ON DELETE CASCADE,
    interest_id BIGINT NOT NULL REFERENCES service.interests(id) ON DELETE CASCADE,
    PRIMARY KEY (bookmark_id, interest_id)
);

-- RSS/API 수집 원천 데이터 (P1이지만 테이블만 미리)
CREATE TABLE service.collected_items (
    id          BIGSERIAL PRIMARY KEY,
    source      VARCHAR(100),                    -- 출처(RSS 피드명 등)
    url         TEXT,
    title       VARCHAR(500),
    content     TEXT,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 브리핑 카드 (사용자 노출 최종 데이터)
CREATE TABLE service.cards (
    id          BIGSERIAL PRIMARY KEY,
    public_id   UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    summary     TEXT,
    why_for_you TEXT,                            -- "왜 당신에게" (agent-contract의 whyForYou)
    visibility  VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
                CHECK (visibility IN ('PRIVATE', 'PUBLIC')),   -- 공개 피드(P1) 스위치
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_cards_user ON service.cards(user_id);

-- 카드 출처 (카드 1장에 여러 출처)
CREATE TABLE service.card_sources (
    id          BIGSERIAL PRIMARY KEY,
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    title       VARCHAR(500),
    url         TEXT
);

-- 피드 아이템 (피드에 어떤 카드가 어떤 상태로 걸리는지)
CREATE TABLE service.feed_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    visibility  VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
                CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_feed_items_user ON service.feed_items(user_id);

-- 좋아요 (P1이지만 테이블만 미리 — SNS 기반)
CREATE TABLE service.likes (
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, card_id)
);

-- 관리자 활동 로그
CREATE TABLE service.admin_logs (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT REFERENCES service.users(id),
    action      VARCHAR(100) NOT NULL,
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- AI 요청 로그 (Service → Agent 요청)
CREATE TABLE service.ai_request_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES service.users(id),
    endpoint    VARCHAR(200) NOT NULL,           -- 예: /agent/bookmarks/process
    request_body JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_request_user ON service.ai_request_logs(user_id);

-- AI 응답 로그 (Agent → Service 응답)
CREATE TABLE service.ai_response_logs (
    id          BIGSERIAL PRIMARY KEY,
    request_id  BIGINT REFERENCES service.ai_request_logs(id) ON DELETE CASCADE,
    status_code INT,
    response_body JSONB,
    latency_ms  INT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- =============================================================
-- agent schema  (AI 파생 데이터. service DB를 직접 수정하지 않는다)
-- 주의: wiki_chunks / embeddings 등 pgvector 필요한 테이블은
--       pgvector 활성화(P1) 시 V2__*.sql 로 추가한다. (여기선 제외)
-- =============================================================

-- 프롬프트 템플릿
CREATE TABLE agent.prompt_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    template    TEXT NOT NULL,
    version     INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 생성 로그 (요약/카드 생성 기록)
CREATE TABLE agent.generation_logs (
    id          BIGSERIAL PRIMARY KEY,
    kind        VARCHAR(50) NOT NULL,            -- 'summary' / 'interest' / 'card'
    input       JSONB,
    output      JSONB,
    model       VARCHAR(100),                    -- 'mock' / 실제 모델명
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 검색 로그 (RAG 검색 기록)
CREATE TABLE agent.retrieval_logs (
    id          BIGSERIAL PRIMARY KEY,
    query       TEXT,
    result_count INT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- =============================================================
-- 기본 데이터 (seed)
-- =============================================================
INSERT INTO service.roles (name) VALUES ('USER'), ('ADMIN');
