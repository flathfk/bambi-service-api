package com.bambi.service.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/** 온보딩에서 선택한 관심 Category·Topic의 service-db 원본. */
@Entity
@Table(name = "user_onboarding_selections")
public class UserOnboardingSelection {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "interest_taxonomy_version", nullable = false, length = 50)
    private String interestTaxonomyVersion;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "selected_category_ids", nullable = false, columnDefinition = "text[]")
    private String[] selectedCategoryIds = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "selected_topic_ids", nullable = false, columnDefinition = "text[]")
    private String[] selectedTopicIds = new String[0];

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserOnboardingSelection() {
    }

    /** 사용자 최초 온보딩 선택을 만든다. */
    public UserOnboardingSelection(Long userId,
                                   String interestTaxonomyVersion,
                                   List<String> selectedCategoryIds,
                                   List<String> selectedTopicIds) {
        this.userId = userId;
        replace(interestTaxonomyVersion, selectedCategoryIds, selectedTopicIds);
    }

    /** 온보딩 선택 전체를 새 분류체계 기준 값으로 교체한다. */
    public void replace(String interestTaxonomyVersion,
                        List<String> selectedCategoryIds,
                        List<String> selectedTopicIds) {
        this.interestTaxonomyVersion = interestTaxonomyVersion;
        this.selectedCategoryIds = selectedCategoryIds.toArray(String[]::new);
        this.selectedTopicIds = selectedTopicIds.toArray(String[]::new);
    }

    public Long getUserId() {
        return userId;
    }

    public String getInterestTaxonomyVersion() {
        return interestTaxonomyVersion;
    }

    public List<String> getSelectedCategoryIds() {
        return List.copyOf(Arrays.asList(selectedCategoryIds));
    }

    public List<String> getSelectedTopicIds() {
        return List.copyOf(Arrays.asList(selectedTopicIds));
    }
}
