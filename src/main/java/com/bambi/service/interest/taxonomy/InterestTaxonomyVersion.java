package com.bambi.service.interest.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Service가 소유하는 관심사 분류체계 버전. 발행된 버전은 수정하지 않는다. */
@Entity
@Table(name = "interest_taxonomy_versions")
public class InterestTaxonomyVersion {

    @Id
    @Column(length = 50)
    private String version;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    protected InterestTaxonomyVersion() {
    }

    public String getVersion() {
        return version;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public String getLocale() {
        return locale;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
