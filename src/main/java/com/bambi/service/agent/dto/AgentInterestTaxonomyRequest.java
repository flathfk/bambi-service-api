package com.bambi.service.agent.dto;

import com.bambi.service.interest.taxonomy.dto.InterestTaxonomyResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Service가 Agent DB에 버전 단위로 복제할 관심사 taxonomy Snapshot. */
public record AgentInterestTaxonomyRequest(
        String version,
        @JsonProperty("source_hash") String sourceHash,
        String locale,
        List<Category> categories) {

    public AgentInterestTaxonomyRequest {
        categories = List.copyOf(categories);
    }

    /** Service taxonomy 응답을 Agent 내부 계약으로 변환한다. */
    public static AgentInterestTaxonomyRequest from(InterestTaxonomyResponse taxonomy) {
        return new AgentInterestTaxonomyRequest(
                taxonomy.version(),
                taxonomy.sourceHash(),
                taxonomy.locale(),
                taxonomy.categories().stream().map(Category::from).toList());
    }

    /** Agent에 전달할 Category Snapshot. */
    public record Category(
            String id,
            String name,
            @JsonProperty("name_en") String nameEn,
            String description,
            String emoji,
            int order,
            List<Topic> topics) {

        public Category {
            topics = List.copyOf(topics);
        }

        /** Service Category를 Agent 계약으로 변환한다. */
        private static Category from(InterestTaxonomyResponse.Category category) {
            return new Category(
                    category.id(),
                    category.name(),
                    category.nameEn(),
                    category.description(),
                    category.emoji(),
                    category.order(),
                    category.topics().stream().map(Topic::from).toList());
        }
    }

    /** Agent 수집 스케줄러가 사용할 Topic Snapshot. */
    public record Topic(
            String id,
            String name,
            @JsonProperty("name_en") String nameEn,
            String description,
            int order,
            List<String> keywords) {

        public Topic {
            keywords = List.copyOf(keywords);
        }

        /** Service Topic을 Agent 계약으로 변환한다. */
        private static Topic from(InterestTaxonomyResponse.Topic topic) {
            return new Topic(
                    topic.id(),
                    topic.name(),
                    topic.nameEn(),
                    topic.description(),
                    topic.order(),
                    topic.keywords());
        }
    }
}
