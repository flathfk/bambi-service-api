package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** LLM Wiki 문서가 참고한 사용자 원본 자료. */
public record WikiDocumentSource(
        @JsonAlias("source_document_id") String sourceDocumentId,
        @JsonAlias("source_type") String sourceType,
        String title,
        @JsonAlias("canonical_url") String canonicalUrl,
        @JsonAlias("relation_type") String relationType) {
}
