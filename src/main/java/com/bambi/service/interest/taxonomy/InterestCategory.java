package com.bambi.service.interest.taxonomy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 관심사 분류체계의 카테고리 행. */
@Entity
@Table(name = "interest_categories")
public class InterestCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxonomy_version", nullable = false, length = 50)
    private String taxonomyVersion;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 300)
    private String description;

    @Column(length = 16)
    private String emoji;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected InterestCategory() {
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
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

    public String getEmoji() {
        return emoji;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
