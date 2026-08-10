-- 생성 접수를 Agent Job·Publish 완료까지 추적하기 위한 종결 상태 정보.
ALTER TABLE service.generation_pendings
    ADD COLUMN error_code VARCHAR(100),
    ADD COLUMN completed_at TIMESTAMPTZ;

CREATE INDEX idx_generation_pendings_poll
    ON service.generation_pendings(status, created_at)
    WHERE status IN ('PENDING', 'RUNNING');
