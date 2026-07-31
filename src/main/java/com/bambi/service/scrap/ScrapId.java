package com.bambi.service.scrap;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link Scrap} 복합키 (@IdClass). service.scraps 의 PK(user_id, card_id) 와 대응.
 */
public class ScrapId implements Serializable {

    private Long userId;
    private Long cardId;

    public ScrapId() {
    }

    public ScrapId(Long userId, Long cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScrapId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, cardId);
    }
}
