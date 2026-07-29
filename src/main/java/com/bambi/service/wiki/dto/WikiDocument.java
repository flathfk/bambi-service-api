package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 저장 자료에서 정리된 위키 문서 한 건 (agent {@code WikiDocumentSummary}).
 * summary 는 AI 요약, domain 은 주제 태그로 쓴다(둘 다 null 가능). documentKind 는 내부 유형(schema 는 서비스가 제외).
 */
public record WikiDocument(
        @JsonAlias("document_id") String documentId,
        @JsonAlias("document_kind") String documentKind,
        String title,
        String summary,
        String domain,
        @JsonAlias("source_count") int sourceCount,
        @JsonAlias("updated_at") String updatedAt) {
}
