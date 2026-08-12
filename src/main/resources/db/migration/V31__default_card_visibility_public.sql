-- =============================================================
-- V31__default_card_visibility_public.sql — 카드 기본 공개범위를 PUBLIC 으로 (2026-08-12 우석 결정)
-- -------------------------------------------------------------
-- V17 에서 이 값을 'PRIVATE' 로 잡았다. 근거는 "공개 피드는 P1" 이었고, 그때는 공개 피드가
-- 없었으니 맞는 기본값이었다.
--
-- 지금은 공개 피드가 살아 있는데 기본이 비공개라, 사용자가 /settings 를 찾아 바꾸지 않는 한
-- 피드에 아무것도 안 뜬다. 서로의 보고서를 보는 것이 이 화면의 존재 이유라 기본값을 뒤집는다.
--
-- ⚠️ V30(변경점 기본 ON)과 달리 **기존 계정의 값도 함께 올린다.** V30 은 "끄기로 선택한 사람의
--    설정을 되돌리게 된다"는 이유로 신규 계정만 바꿨는데, 여기서는 그 논리가 성립하지 않는다:
--    이 컬럼은 V17 이후 줄곧 'PRIVATE' 이 기본이라, 지금 PRIVATE 인 계정은 **직접 고른 사람과
--    한 번도 안 건드린 사람이 구분되지 않는다.** 신규 가입자만 바꾸면 기존 사용자의 피드는
--    계속 비어 있어서 변경의 목적 자체가 사라진다.
--    직접 'PUBLIC' 을 고른 계정은 이미 PUBLIC 이므로 이 UPDATE 의 영향을 받지 않는다.
--
-- ⚠️ **이미 발행된 카드는 건드리지 않는다.** 이 마이그레이션은 앞으로 발행되는 카드의
--    최초 공개범위만 바꾼다. 과거 카드를 소급 공개하면 사용자가 비공개로 남겨둔 글이
--    예고 없이 공개된다.
--
-- ⚠️ 아침 브리핑은 이 설정과 무관하게 계속 PRIVATE 이다(#100 · PublishProcessingService
--    .initialVisibility). 공개 대상은 온디맨드·온보딩 등 나머지 유형이다.
--
-- ⚠️ 엔티티 @Column 과 기본값 정합 필수:
--   default_card_visibility VARCHAR(20) DEFAULT 'PUBLIC' ↔ User.defaultCardVisibility = "PUBLIC"
-- =============================================================

ALTER TABLE service.users
    ALTER COLUMN default_card_visibility SET DEFAULT 'PUBLIC';

-- 기존 계정도 기본값을 따라 올린다(위 주석의 근거). PUBLIC 을 직접 고른 계정은 그대로다.
UPDATE service.users
   SET default_card_visibility = 'PUBLIC'
 WHERE default_card_visibility = 'PRIVATE';

COMMENT ON COLUMN service.users.default_card_visibility IS
    '카드 발행 시 기본 공개범위(PRIVATE|PUBLIC). 기본 PUBLIC(2026-08-12 V31 — 공개 피드가 채워지도록 opt-out 으로 전환). 아침 브리핑은 이 값과 무관하게 항상 PRIVATE.';
