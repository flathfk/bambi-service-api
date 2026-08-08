-- =============================================================
-- V17__briefing_topics.sql — 아침 브리핑 관심사 선택값 (2026-08-07 저녁 송우·기용 확정)
-- -------------------------------------------------------------
-- 아침 브리핑은 사용자가 미리 고른 관심사 3개(topics)로 만들어진다.
-- 그 선택값을 담는다. 프론트 선택 화면은 bambi-service-web #59(여진).
--
-- ⚠️ topic(이름)으로 저장한다. agent 의 interest_id(UUID) 로 저장하면 안 된다.
--   관심 Profile 은 재계산할 때마다 기존 active 를 retired 로 내리고 새 Profile 을 만드는데,
--   user_interests 가 profile_id 에 CASCADE 로 묶이고 PK 가 gen_random_uuid() 라
--   재계산 때마다 모든 interest_id 가 새로 발급된다. UUID 로 저장하면 사용자 선택이
--   다음 재계산에 retired 를 가리켜 409 ACTIVE_INTEREST_REQUIRED 로 조용히 죽는다.
--   agent 계약이 topics[] = 이름 문자열 배열이라 이름이 그대로 전송값이기도 하다.
--
-- position: agent 계약상 "topics 순서가 곧 리포트 안 섹션 순서"라 순서를 보존해야 한다.
-- 길이 500: agent GenerationRequest.topics 각 항목 상한과 맞춘다.
-- 행이 없으면 = 미선택. 그때는 등록 관심사로 폴백한다(agent-api #20 폴백 3단계).
--
-- 번호 조율: V16 = 우석 oauth_hash_column_type 까지 사용됨. flyway out-of-order 켜져 있음
-- (application.yml) — 병렬 작업이라 머지 순서가 번호 순서와 다를 수 있다. 앞 버전 수정 금지.
-- =============================================================

CREATE TABLE service.user_briefing_topics (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    position   INT NOT NULL,
    topic      VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, position)
);

CREATE INDEX idx_user_briefing_topics_user
    ON service.user_briefing_topics(user_id, position);

COMMENT ON TABLE service.user_briefing_topics IS
    '아침 브리핑용 사용자 선택 관심사. agent topics[] 로 그대로 전송된다. 행 없음 = 미선택(폴백).';
COMMENT ON COLUMN service.user_briefing_topics.topic IS
    '관심사 이름. agent interest_id(UUID) 아님 — Profile 재계산마다 UUID 가 새로 발급돼 썩는다.';
COMMENT ON COLUMN service.user_briefing_topics.position IS
    '0부터. agent 계약상 topics 순서 = 리포트 섹션 순서라 보존한다.';
