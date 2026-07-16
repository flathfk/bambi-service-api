package com.bambi.service.bookmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * 저장한 URL/본문 (service.bookmarks). 원본 데이터 — Service Layer 가 source of truth.
 * summary 는 Agent 가 만든 요약을 Service 가 저장하는 컬럼 (Agent 는 service DB 를 직접 만지지 않는다).
 * user_id 는 note 템플릿 패턴대로 단순 컬럼 — 소유자 검증은 쿼리에서 강제한다.
 */
@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private String url;

    @Column(length = 500)
    private String title;

    @Column
    private String content;

    @Column
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookmarkStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Bookmark() {
    }

    public Bookmark(Long userId, String url, String title, String content) {
        this.userId = userId;
        this.url = url;
        this.title = title;
        this.content = content;
        this.status = BookmarkStatus.PROCESSING;
    }

    /** Agent 처리 성공 — 요약 반영 + DONE 전이 */
    public void completeProcessing(String summary) {
        this.summary = summary;
        this.status = BookmarkStatus.DONE;
    }

    /** Agent 처리 실패 — 재시도(P1) 대상 표시 */
    public void failProcessing() {
        this.status = BookmarkStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public BookmarkStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
