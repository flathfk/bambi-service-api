package com.bambi.service.card;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 브리핑 카드 — 사용자에게 노출되는 최종 데이터 (service.cards).
 * 대외 식별자는 public_id(UUID) 를 쓴다 (순번 노출 방지 — V1 설계 의도).
 * 출처(card_sources)는 카드가 소유하는 aggregate 로, 카드 저장 시 함께 저장된다.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column
    private String summary;

    @Column(name = "why_for_you")
    private String whyForYou;

    @Column(nullable = false, length = 20)
    private String visibility;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardSource> sources = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Card() {
    }

    public Card(Long userId, String title, String summary, String whyForYou) {
        this.publicId = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.summary = summary;
        this.whyForYou = whyForYou;
        this.visibility = "PRIVATE";   // 공개 피드는 P1 — 기본 비공개
    }

    public void addSource(String title, String url) {
        sources.add(new CardSource(this, title, url));
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getWhyForYou() {
        return whyForYou;
    }

    public List<CardSource> getSources() {
        return sources;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
