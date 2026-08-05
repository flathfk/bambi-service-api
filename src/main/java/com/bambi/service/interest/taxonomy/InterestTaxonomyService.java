package com.bambi.service.interest.taxonomy;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.interest.taxonomy.dto.InterestTaxonomyResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 활성 관심사 분류체계를 조회하고 사용자 선택 ID를 검증한다. */
@Service
public class InterestTaxonomyService {

    private final InterestTaxonomyVersionRepository versionRepository;
    private final InterestCategoryRepository categoryRepository;
    private final InterestTopicRepository topicRepository;

    public InterestTaxonomyService(
            InterestTaxonomyVersionRepository versionRepository,
            InterestCategoryRepository categoryRepository,
            InterestTopicRepository topicRepository) {
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
    }

    /** 현재 활성 taxonomy 전체를 카테고리 순서대로 반환한다. */
    @Transactional(readOnly = true)
    public InterestTaxonomyResponse getActiveTaxonomy() {
        InterestTaxonomyVersion version = versionRepository
                .findFirstByStatusOrderByPublishedAtDesc("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("활성 관심사 분류체계가 없습니다."));
        var categories = categoryRepository
                .findByTaxonomyVersionOrderByDisplayOrder(version.getVersion());
        var topicsByCategory = topicRepository.findByTaxonomyVersion(version.getVersion()).stream()
                .sorted(Comparator.comparingInt(InterestTopic::getDisplayOrder))
                .collect(Collectors.groupingBy(InterestTopic::getCategoryKey));
        var categoryViews = categories.stream()
                .map(category -> new InterestTaxonomyResponse.Category(
                        category.getCategoryKey(),
                        category.getName(),
                        category.getNameEn(),
                        category.getDescription(),
                        category.getEmoji(),
                        category.getDisplayOrder(),
                        topicsByCategory.getOrDefault(category.getCategoryKey(), java.util.List.of()).stream()
                                .map(topic -> new InterestTaxonomyResponse.Topic(
                                        topic.getTopicKey(),
                                        topic.getName(),
                                        topic.getNameEn(),
                                        topic.getDescription(),
                                        topic.getDisplayOrder(),
                                        topic.getKeywords()))
                                .toList()))
                .toList();
        return new InterestTaxonomyResponse(
                version.getVersion(),
                version.getSourceHash(),
                version.getLocale(),
                version.getPublishedAt(),
                categoryViews);
    }

    /** 전달된 버전·토픽 ID를 활성 taxonomy의 정식 토픽으로 해석한다. */
    @Transactional(readOnly = true)
    public ResolvedTopic resolveActiveTopic(String version, String topicId) {
        InterestTaxonomyResponse active = getActiveTaxonomy();
        if (!active.version().equals(version)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "현재 관심사 분류체계 버전이 아닙니다.");
        }
        Map<String, InterestTaxonomyResponse.Category> categories = active.categories().stream()
                .collect(Collectors.toMap(InterestTaxonomyResponse.Category::id, Function.identity()));
        for (InterestTaxonomyResponse.Category category : categories.values()) {
            for (InterestTaxonomyResponse.Topic topic : category.topics()) {
                if (topic.id().equals(topicId)) {
                    return new ResolvedTopic(
                            active.version(), category.id(), category.name(), topic.id(), topic.name());
                }
            }
        }
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 관심사 토픽입니다.");
    }

    /** 사용자 선택 저장과 Agent Snapshot 조립에 쓰는 정규화된 토픽. */
    public record ResolvedTopic(
            String taxonomyVersion,
            String categoryId,
            String categoryName,
            String topicId,
            String topicName) {
    }
}
