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

    public void rename(String name) {
        this.name = name;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
