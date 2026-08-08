-- =============================================================
-- V17__user_settings.sql — 사용자 설정(기본 카드 공개범위 · 리포트 완료 알림 수신)
-- -------------------------------------------------------------
-- 근거: 설정 화면(여진, 08-07). PATCH /api/users/me/settings 로 둘 다 변경, GET /api/auth/me 로 조회.
-- 소유: 사용자 설정 = 영현.
--
-- ⚠️ 엔티티 @Column 과 타입·기본값 정합 필수 (V15 CHAR(64) vs varchar → ddl-auto=validate 502 교훈):
--   default_card_visibility   VARCHAR(20)  ↔ User.defaultCardVisibility  (String, @Column length=20)  기본 'PRIVATE'
--   report_ready_notification BOOLEAN      ↔ User.reportReadyNotification (boolean)                     기본 true
-- 엔티티 필드 초기값을 DB DEFAULT 와 동일하게 둔다 — JPA insert 가 DEFAULT 를 Java 기본값(null/false)으로
-- 덮어써 NOT NULL 위반/의도치 않은 false 저장이 나지 않게.
--
-- 번호: 병합된 최대 V16 다음 = V17 (V10 은 기존 결번). Flyway out-of-order ON.
-- 배포된 앞 버전 수정 금지, 추가만.
-- =============================================================

ALTER TABLE service.users
    ADD COLUMN default_card_visibility   VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN report_ready_notification BOOLEAN     NOT NULL DEFAULT TRUE;

-- cards.visibility 와 동일 규칙(PRIVATE|PUBLIC)을 DB 로도 이중 방어.
ALTER TABLE service.users
    ADD CONSTRAINT users_default_card_visibility_check
        CHECK (default_card_visibility IN ('PRIVATE', 'PUBLIC'));

COMMENT ON COLUMN service.users.default_card_visibility   IS '카드 발행 시 기본 공개범위(PRIVATE|PUBLIC). 기본 PRIVATE.';
COMMENT ON COLUMN service.users.report_ready_notification IS '리포트 완료(REPORT_READY) 알림 수신 여부. false 면 알림을 생성하지 않는다.';
