package com.bambi.service.interest.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/** 관심사 분류체계의 검색 가능한 토픽 행. */
@Entity
@Table(name = "interest_topics")
public class InterestTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxonomy_version", nullable = false, length = 50)
    private String taxonomyVersion;

    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 300)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> keywords;

    protected InterestTopic() {
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public String getTopicKey() {
        return topicKey;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public List<String> getKeywords() {
        return List.copyOf(keywords);
    }
}
