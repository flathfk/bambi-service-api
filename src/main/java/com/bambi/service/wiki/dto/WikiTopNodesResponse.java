package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * agent 개인 Wiki 연결 상위 Node 조회 중계 응답 (GET /api/wiki/graph/top-nodes).
 * agent {@code WikiTopNodesResponse}(snake_case)를 camelCase 로 노출한다. 관심사 화면의 "연결 많은 주제" 위젯용.
 */
public record WikiTopNodesResponse(
        @JsonAlias("total_node_count") int totalNodeCount,
        List<WikiTopNode> items) {
}
