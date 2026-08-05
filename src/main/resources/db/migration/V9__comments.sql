-- =============================================================
-- V9__comments.sql — 카드 댓글 (Week3 SNS)
-- -------------------------------------------------------------
-- 공개(PUBLIC) 카드에 다는 댓글. 작성자만 삭제(soft delete — 다른 도메인과 동일 정책).
-- 마이그레이션 번호: V6=agent_context_version, V7=scraps, V8=user_profile 까지 머지됨 → 댓글은 V9.
-- Flyway: 앞 버전 수정 금지, 추가만.
-- =============================================================

CREATE TABLE service.comments (
    id          BIGSERIAL PRIMARY KEY,
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,   -- 작성자
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
-- 카드별 댓글 목록(오래된 순 = 대화 흐름) 조회용. soft delete 는 서비스 WHERE 로 거른다.
CREATE INDEX idx_comments_card ON service.comments(card_id, created_at);
