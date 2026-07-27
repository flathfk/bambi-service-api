package com.bambi.service.like;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link Like} 복합키 (@IdClass). service.likes 의 PK(user_id, card_id) 와 대응.
 */
public class LikeId implements Serializable {

    private Long userId;
    private Long cardId;

    public LikeId() {
    }

    public LikeId(Long userId, Long cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LikeId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, cardId);
    }
}
