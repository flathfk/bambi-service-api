-- =============================================================
-- V4__sns_follows.sql — SNS(Week2): 팔로우 관계 + 공개피드/좋아요 인덱스 (영현)
-- -------------------------------------------------------------
-- 범위: 팔로우(follows) 신규 + 공개피드/좋아요 조회 성능 인덱스.
--   likes 테이블과 cards.visibility 는 V1 에 이미 있으므로 재생성하지 않는다.
--   feed_items 는 P1 이라 손대지 않는다(공개피드는 cards.visibility 로 관통).
--
-- Flyway 버전 조율: 이 파일은 V4(영현·SNS). 소라의 agent_context_version 컬럼은
--   충돌 방지를 위해 V5 로 올린다. (동일 버전 번호 중복 금지 — Flyway checksum 깨짐)
--
-- 무결성(가이드 §7): 팔로우는 users 참조 FK + 자기팔로우 CHECK 로 유령/자기참조 원천 차단.
--   soft delete 대신 하드 삭제(언팔 = row DELETE) — likes 와 동일한 순수 조인 테이블 성격.
-- =============================================================

-- 팔로우 (follower_id 가 followee_id 를 팔로우한다)
CREATE TABLE service.follows (
    follower_id  BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    followee_id  BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),                       -- (팔로워,대상) 유일 = 멱등 팔로우의 물리적 근거
    CONSTRAINT chk_follows_no_self CHECK (follower_id <> followee_id)  -- 자기 자신 팔로우 원천 차단
);
-- "나를 팔로우하는 사람" 조회용. (팔로워→ 조회는 PK 선두 컬럼으로 커버됨)
CREATE INDEX idx_follows_followee ON service.follows(followee_id);

-- 공개 피드 조회 최적화: PUBLIC 카드만 최신순으로 훑는다(살아있는 것만).
CREATE INDEX idx_cards_public_feed
    ON service.cards(created_at DESC)
    WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;

-- 카드별 좋아요 수 집계(공개피드 렌더링) 최적화.
CREATE INDEX idx_likes_card ON service.likes(card_id);
