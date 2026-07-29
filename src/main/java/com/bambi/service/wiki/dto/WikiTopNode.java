package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 연결 수 상위 위키 문서 요약 (agent {@code WikiTopNode}). degree = 연결된 Edge 수. */
public record WikiTopNode(
        int rank,
        @JsonAlias("document_id") String documentId,
        String title,
        int degree,
        String summary) {
}
