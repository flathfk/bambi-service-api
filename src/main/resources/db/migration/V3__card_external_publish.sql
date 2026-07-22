-- =============================================================
-- V3__card_external_publish.sql — 비동기 발행(agent Pull) 대비 카드 멱등 키
-- -------------------------------------------------------------
-- 배경: agent-api 는 Pull 구조(202+job_id, publish-snapshot claim/ack).
--   service-worker(폴링 워커)가 완성 콘텐츠를 service-db 로 옮길 때
--   docs/service-integration-guide.md §4 규칙대로 "content_id + version 키로
--   멱등 Upsert" 해야 한다 (재-claim/재-ack 에도 카드 중복 생성 금지).
-- 설계: 기존 동기 "즉시 카드" 경로는 이 두 컬럼을 NULL 로 둔다(무영향).
--   외부(agent) 발행 카드만 external_content_id/version 을 채운다.
-- Flyway: V1 은 배포 후 동결. 이 추가는 컬럼 add + 부분 유니크 인덱스로 후방호환.
-- =============================================================

ALTER TABLE service.cards
    ADD COLUMN external_content_id VARCHAR(200),   -- agent content_id (NULL = 동기 생성 카드)
    ADD COLUMN external_version    INT;            -- agent snapshot version

-- 같은 사용자 + 같은 external_content_id 는 카드 1장 (멱등 Upsert 의 물리적 근거).
-- 부분 인덱스라 동기 카드(둘 다 NULL)에는 영향 없음.
CREATE UNIQUE INDEX uq_cards_user_external
    ON service.cards(user_id, external_content_id)
    WHERE external_content_id IS NOT NULL;
