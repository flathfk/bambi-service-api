package com.bambi.service.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 카드 댓글 (service.comments). 공개 카드에 작성, 작성자만 soft delete.
 * card_id·user_id 는 note 템플릿 패턴대로 단순 컬럼 — 소유자 검증은 쿼리에서 강제.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Comment() {
    }

    public Comment(Long cardId, Long userId, String content) {
        this.cardId = cardId;
        this.userId = userId;
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCardId() {
        return cardId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
