-- 정기 아침 브리핑을 미리 저장하되 07시 전에는 어떤 카드 조회에도 노출하지 않는다.

ALTER TABLE service.cards
    ADD COLUMN available_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX idx_cards_user_available
    ON service.cards(user_id, available_at, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cards_public_available
    ON service.cards(visibility, available_at, created_at DESC)
    WHERE deleted_at IS NULL;
