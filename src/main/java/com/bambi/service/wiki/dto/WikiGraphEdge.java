package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 개인 Wiki 문서 두 개를 잇는 방향성 관계. */
public record WikiGraphEdge(
        String id,
        String source,
        String target,
        @JsonAlias("relation_type") String relationType) {
}
