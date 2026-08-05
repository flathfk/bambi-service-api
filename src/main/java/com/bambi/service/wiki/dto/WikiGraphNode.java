package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/** Service Web이 시각화할 개인 Wiki Entity·Concept Node. */
public record WikiGraphNode(
        String id,
        @JsonAlias("document_kind") String documentKind,
        @JsonAlias("document_key") String documentKey,
        String title,
        String subtype,
        String summary,
        List<String> aliases,
        @JsonAlias("file_path") String filePath,
        int version,
        @JsonAlias("updated_at") String updatedAt,
        int degree) {
}
