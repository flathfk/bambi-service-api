package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/** 인증 사용자의 LLM Wiki 전체 Graph 중계 응답. */
public record WikiGraphResponse(
        @JsonAlias("wiki_version") Integer wikiVersion,
        @JsonAlias("generated_at") String generatedAt,
        WikiGraphStats stats,
        List<WikiGraphNode> nodes,
        List<WikiGraphEdge> edges) {
}
