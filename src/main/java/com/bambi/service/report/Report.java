package com.bambi.service.report;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 보고서 본문 (service.reports) — agent 생성 콘텐츠의 body 보존처.
 * 카드(cards)는 이 리포트의 요약본이고, 본문·인용은 여기에 담는다(agent-contract §3.1).
 * service-worker(claim)가 content_id/version 멱등 키로 리포트+카드를 함께 upsert 한다.
 * 대외 식별자는 publicId(UUID) — 카드와 동일 컨벤션.
 */
@Entity
@Table(name = "reports")
public class Report {

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

    @Column
    private String body;

    @Column(name = "external_content_id", length = 200)
    private String externalContentId;

    @Column(name = "external_version")
    private Integer externalVersion;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<ReportCitation> citations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Report() {
    }

    /** agent 발행(Pull) 리포트 — content_id/version 멱등 키를 함께 보관한다. */
    public static Report fromExternal(Long userId, String contentId, Integer version,
                                      String title, String summary, String body) {
        Report report = new Report();
        report.publicId = UUID.randomUUID();
        report.userId = userId;
        report.externalContentId = contentId;
        report.externalVersion = version;
        report.title = title;
        report.summary = summary;
        report.body = body;
        return report;
    }

    public void addCitation(String title, String url) {
        citations.add(new ReportCitation(this, title, url));
    }

    /** 발행 재수신(더 큰 version)으로 본문을 갱신. 인용은 통째로 교체한다. */
    public void updateBody(Integer version, String title, String summary, String body) {
        this.externalVersion = version;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.citations.clear();
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

    public String getBody() {
        return body;
    }

    public Integer getExternalVersion() {
        return externalVersion;
    }

    public List<ReportCitation> getCitations() {
        return citations;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
