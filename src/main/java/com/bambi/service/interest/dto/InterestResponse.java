package com.bambi.service.interest.dto;

import com.bambi.service.interest.Interest;
import com.bambi.service.interest.InterestSource;

import java.time.OffsetDateTime;

public record InterestResponse(
        Long id,
        String name,
        InterestSource source,
        String taxonomyVersion,
        String categoryId,
        String topicId,
        OffsetDateTime createdAt) {

    public static InterestResponse from(Interest interest) {
        return new InterestResponse(
                interest.getId(),
                interest.getName(),
                interest.getSource(),
                interest.getTaxonomyVersion(),
                interest.getTaxonomyCategoryId(),
                interest.getTaxonomyTopicId(),
                interest.getCreatedAt());
    }
}
