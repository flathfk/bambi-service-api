package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 개인 Wiki Graph의 Node·Edge 종류별 집계. */
public record WikiGraphStats(
        @JsonAlias("node_count") int nodeCount,
        @JsonAlias("edge_count") int edgeCount,
        @JsonAlias("entity_count") int entityCount,
        @JsonAlias("concept_count") int conceptCount,
        @JsonAlias("orphan_count") int orphanCount) {
}
