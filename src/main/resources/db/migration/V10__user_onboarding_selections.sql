-- =============================================================
-- V10__user_onboarding_selections.sql — 온보딩 관심 분류 선택
-- -------------------------------------------------------------
-- service-db가 원천인 Category·Topic 선택을 사용자별 한 행으로 보존한다.
-- 배열 순서는 온보딩 UI의 선택 순서를 유지하며 Agent Context Snapshot에
-- 같은 안정 ID와 taxonomy_version을 그대로 전달한다.
-- =============================================================

CREATE TABLE service.user_onboarding_selections (
    user_id                 BIGINT PRIMARY KEY REFERENCES service.users(id) ON DELETE CASCADE,
    interest_taxonomy_version VARCHAR(50) NOT NULL
                              CHECK (length(btrim(interest_taxonomy_version)) > 0),
    selected_category_ids   TEXT[] NOT NULL DEFAULT '{}',
    selected_topic_ids      TEXT[] NOT NULL DEFAULT '{}',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (cardinality(selected_category_ids) <= 8),
    CHECK (cardinality(selected_topic_ids) <= 12)
);

COMMENT ON TABLE service.user_onboarding_selections IS
    '온보딩에서 사용자가 선택한 관심 Category·Topic 안정 ID의 원본';
