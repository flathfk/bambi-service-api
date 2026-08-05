package com.bambi.service.interest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 관심사 (service.interests). 사용자 소유 데이터.
 * V1 스키마 주의: notes 와 달리 updated_at 컬럼이 없다 → @UpdateTimestamp 두지 않는다.
 * user_id 는 note 템플릿 패턴대로 단순 컬럼 — 소유자 검증은 쿼리에서 강제.
 */
@Entity
@Table(name = "interests")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "taxonomy_version", length = 50)
    private String taxonomyVersion;

    @Column(name = "taxonomy_category_id", length = 50)
    private String taxonomyCategoryId;

    @Column(name = "taxonomy_topic_id", length = 50)
    private String taxonomyTopicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterestSource source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Interest() {
    }

    /** 사용자 직접 입력 관심사 (source=USER). agent 추론(INFERRED)은 이 경로로 안 만든다. */
    public Interest(Long userId, String name) {
        this.userId = userId;
        this.name = name;
        this.source = InterestSource.USER;
    }

    /** Service taxonomy에서 고른 정식 토픽 관심사를 만든다. */
    public static Interest fromTaxonomy(
            Long userId,
            String name,
            String taxonomyVersion,
            String categoryId,
            String topicId) {
        Interest interest = new Interest(userId, name);
        interest.taxonomyVersion = taxonomyVersion;
        interest.taxonomyCategoryId = categoryId;
        interest.taxonomyTopicId = topicId;
        return interest;
    }

    public void rename(String name) {
        this.name = name;
        this.taxonomyVersion = null;
        this.taxonomyCategoryId = null;
        this.taxonomyTopicId = null;
    }

    /** 선택을 정식 taxonomy 토픽으로 교체한다. */
    public void selectTaxonomyTopic(
            String name,
            String taxonomyVersion,
            String categoryId,
            String topicId) {
        this.name = name;
        this.taxonomyVersion = taxonomyVersion;
        this.taxonomyCategoryId = categoryId;
        this.taxonomyTopicId = topicId;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public InterestSource getSource() {
        return source;
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public String getTaxonomyCategoryId() {
        return taxonomyCategoryId;
    }

    public String getTaxonomyTopicId() {
        return taxonomyTopicId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
