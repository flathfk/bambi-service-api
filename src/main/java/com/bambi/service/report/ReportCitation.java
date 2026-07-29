package com.bambi.service.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 리포트 출처(인용) (service.report_citations). 리포트 1건에 여러 출처.
 * 카드의 card_sources 와 별개 — 리포트 본문의 근거(§3.1).
 */
@Entity
@Table(name = "report_citations")
public class ReportCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(length = 500)
    private String title;

    @Column
    private String url;

    protected ReportCitation() {
    }

    ReportCitation(Report report, String title, String url) {
        this.report = report;
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
