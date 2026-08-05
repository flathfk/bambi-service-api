package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 상세 Wiki 문서와 연결된 다른 Entity·Concept Node. */
public record WikiDocumentRelation(
        String direction,
        @JsonAlias("related_document_id") String relatedDocumentId,
        @JsonAlias("related_document_kind") String relatedDocumentKind,
        @JsonAlias("related_title") String relatedTitle,
        @JsonAlias("relation_type") String relationType) {
}
