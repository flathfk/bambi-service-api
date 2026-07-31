-- =============================================================
-- V7__scraps.sql — 스크랩(남의 공개 카드 담기) (Week3 SNS)
-- -------------------------------------------------------------
-- 스크랩 = 남이 공개한 카드를 내 보관함에 담는 것. 07-27 확정대로 관심 자료 저장(bookmarks)과는
-- 다른 개념이라 bookmarks 재사용 금지, 별도 테이블로 신설한다.
-- 좋아요(service.likes)와 같은 구조: (user_id, card_id) 복합키, 하드 삭제, 멱등.
--
-- 마이그레이션 번호: V6 = 소라 agent_context_version 예약(비워둠), V7 = 스크랩(영현), V8 = 소라 users.bio.
-- Flyway: 앞 버전 수정 금지, 추가만.
-- =============================================================

CREATE TABLE service.scraps (
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    card_id     BIGINT NOT NULL REFERENCES service.cards(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, card_id)
);
-- 내 스크랩 목록(최신순) 조회용.
CREATE INDEX idx_scraps_user ON service.scraps(user_id, created_at DESC);
