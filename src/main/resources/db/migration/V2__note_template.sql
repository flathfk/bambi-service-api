-- =============================================================
-- V2__note_template.sql — reference CRUD 템플릿용 중립 엔티티
-- -------------------------------------------------------------
-- 목적: 팀원이 Controller/Service/Repository/DTO/공통응답/예외/권한 1세트를
--       "복붙 시작점"으로 삼도록, 도메인과 무관한 note 테이블을 제공한다.
--       (Bookmark/Card 등 실제 도메인은 이 패턴을 참고해 각자 구현)
--
-- Flyway 규칙: V1 은 첫 배포 전까지만 수정 가능. init 이후 변경은 이렇게 V2+ 로 추가.
-- =============================================================

CREATE TABLE service.notes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ                              -- soft delete (NULL = 살아있음)
);
CREATE INDEX idx_notes_user ON service.notes(user_id);
