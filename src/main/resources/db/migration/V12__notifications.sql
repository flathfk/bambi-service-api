-- Agent 발행물이 Service DB에 반영된 뒤 사용자에게 노출할 알림 Inbox.

CREATE TABLE service.notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    event_key       VARCHAR(250) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(500),
    target_path     VARCHAR(500),
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, event_key)
);

CREATE INDEX idx_notifications_user_created
    ON service.notifications(user_id, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON service.notifications(user_id, created_at DESC)
    WHERE read_at IS NULL;
