package com.bambi.service.interest.taxonomy.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** 프론트와 Agent 동기화에 공통으로 쓰는 활성 관심사 분류체계 응답. */
public record InterestTaxonomyResponse(
        String version,
        String sourceHash,
        String locale,
        OffsetDateTime publishedAt,
        List<Category> categories) {

    public InterestTaxonomyResponse {
        categories = List.copyOf(categories);
    }

    /** 카테고리와 그 하위 토픽. */
    public record Category(
            String id,
            String name,
            String nameEn,
            String description,
            String emoji,
            int order,
            List<Topic> topics) {

        public Category {
            topics = List.copyOf(topics);
        }
    }

    /** 수집 키워드를 포함한 토픽. */
    public record Topic(
            String id,
            String name,
            String nameEn,
            String description,
            int order,
            List<String> keywords) {

        public Topic {
            keywords = List.copyOf(keywords);
        }
    }
}
