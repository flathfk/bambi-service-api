-- =============================================================
-- V5__reports.sql — agent 생성 본문(리포트) 보존 + 카드 관심사 태그
-- -------------------------------------------------------------
-- 근거: agent-contract §3.1 (2026-07-22 확정, (C) 비동기).
--   · body 는 카드가 아니라 service.reports 에 보존한다(카드는 요약 전용).
--   · cards 는 report_id 로 리포트를 참조한다.
--   · why_for_you 는 폐기 방향 → 카드 관심사 태그(topic 문자열)로 대체.
--     (기존 즉시 카드 경로가 아직 why_for_you 를 쓰므로 컬럼은 남기고 태그를 추가한다)
-- 소유: report/카드 태그 스키마 = 영현·우석. Pull(claim) 저장 배선은 후속 작업.
--
-- main 은 V4(SNS)까지라 이 파일은 V5 다. (로드맵의 "V4" 표기는 SNS 머지 전 기준)
-- Flyway: 배포된 앞 버전은 수정 금지, 추가만.
-- =============================================================

-- 보고서(본문) — 사용자 노출 최종 데이터. 카드는 이 리포트의 요약본.
CREATE TABLE service.reports (
    id                  BIGSERIAL PRIMARY KEY,
    public_id           UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),  -- 대외 노출용(상세 조회)
    user_id             BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    title               VARCHAR(500) NOT NULL,
    summary             TEXT,
    body                TEXT,                       -- 카드엔 없고 리포트에 보존(§3.1)
    -- 발행 멱등 키 (claim upsert). cards.external_* 와 동일 개념.
    external_content_id VARCHAR(200),
    external_version    INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_reports_user ON service.reports(user_id);
-- 같은 사용자 + 같은 external_content_id 는 리포트 1건 (멱등 Upsert 근거).
CREATE UNIQUE INDEX uq_reports_user_external
    ON service.reports(user_id, external_content_id) WHERE external_content_id IS NOT NULL;

-- 리포트 출처(인용). 카드의 card_sources 와 별개 — 리포트 본문의 근거.
CREATE TABLE service.report_citations (
    id          BIGSERIAL PRIMARY KEY,
    report_id   BIGINT NOT NULL REFERENCES service.reports(id) ON DELETE CASCADE,
    title       VARCHAR(500),
    url         TEXT
);
CREATE INDEX idx_report_citations_report ON service.report_citations(report_id);

-- 카드 → 리포트 참조 (카드는 요약 전용, 본문은 리포트에).
ALTER TABLE service.cards ADD COLUMN report_id BIGINT REFERENCES service.reports(id);
CREATE INDEX idx_cards_report ON service.cards(report_id);

-- 카드 관심사 태그 (why_for_you 폐기 대체) — agent 가 카드에 붙인 topic 문자열.
-- 온보딩 상위 분야(category) 분류체계와는 다른 층위(토픽 수준). category enum 은 별도 트랙.
CREATE TABLE service.card_interest_tags (
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (card_id, tag)
);
