package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/** 현재 LLM Wiki Node의 Markdown·원본 출처·연결 관계 상세. */
public record WikiDocumentDetailResponse(
        @JsonAlias("document_id") String documentId,
        @JsonAlias("document_version_id") String documentVersionId,
        @JsonAlias("document_kind") String documentKind,
        @JsonAlias("document_key") String documentKey,
        @JsonAlias("file_path") String filePath,
        String domain,
        String title,
        String summary,
        int version,
        @JsonAlias("source_count") int sourceCount,
        @JsonAlias("updated_at") String updatedAt,
        String markdown,
        List<WikiDocumentSource> sources,
        List<WikiDocumentRelation> relations) {
}
