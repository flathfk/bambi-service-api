-- =============================================================
-- V19__change_history_setting.sql — 변경점(Delta) 추적 계정 설정 (2026-08-10 김기용 요청)
-- -------------------------------------------------------------
-- 델타 리포트를 요청 단위 토글에서 **계정 단위 설정**으로 전환한다.
-- PATCH /api/users/me/settings 로 변경, GET /api/auth/me 로 조회 (V17 설정 패턴 그대로).
-- 온디맨드 생성 시 이 값을 change_history_enabled 로 agent 에 싣는다.
--
-- 기본 FALSE: 델타 경로는 일반 생성보다 LLM 호출이 많다(팩트 추출·종합·파급효과 워커) —
-- 비용이 더 드는 경로는 사용자가 명시적으로 켠다(김기용 트레이드오프 안내 08-10).
--
-- ⚠️ 엔티티 @Column 과 타입·기본값 정합 필수 (V15 CHAR(64) vs varchar → ddl-auto=validate 502 교훈):
--   change_history_enabled BOOLEAN ↔ User.changeHistoryEnabled (boolean) 기본 false
--
-- 번호: 병합된 최대 V18 다음 = V19. ⚠️ 머지 전 다른 오픈 PR 의 마이그레이션 번호와 수동 대조할 것
-- (V17 이 #63/#65 에서 겹쳤던 사례 — 파일명이 달라 git·CI 가 못 잡고 배포에서 Flyway 가 죽는다).
-- =============================================================

ALTER TABLE service.users
    ADD COLUMN change_history_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN service.users.change_history_enabled IS
    '변경점(Delta) 추적 리포트 계정 설정. true 면 온디맨드 생성 요청에 change_history_enabled 를 실어 보낸다. 기본 false(비용 큰 경로는 명시적 opt-in).';
