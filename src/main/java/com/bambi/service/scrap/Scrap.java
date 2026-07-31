package com.bambi.service.scrap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 스크랩 (service.scraps) — user_id 가 card_id(남의 공개 카드)를 담았다.
 * 좋아요(likes)와 같은 구조(복합키, 하드 삭제, 멱등). 관심 자료 저장(bookmarks)과는 다른 개념.
 */
@Entity
@Table(name = "scraps")
@IdClass(ScrapId.class)
public class Scrap {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "card_id")
    private Long cardId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Scrap() {
    }

    public Scrap(Long userId, Long cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCardId() {
        return cardId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
