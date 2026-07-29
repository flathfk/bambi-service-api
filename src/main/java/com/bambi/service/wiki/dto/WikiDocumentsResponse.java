package com.bambi.service.wiki.dto;

import java.util.List;

/**
 * agent 개인 Wiki 문서 목록 조회 중계 응답 (GET /api/wiki/documents).
 * agent {@code WikiDocumentListResponse}(snake_case)를 camelCase 로 노출한다.
 * 내부용 {@code schema} 문서는 {@link #withoutSchema()} 로 걸러 사용자 화면에 안 보인다.
 */
public record WikiDocumentsResponse(int total, List<WikiDocument> items) {

    /** 내부 {@code schema}(root 등) 문서를 제외한 목록으로 다시 만든다. total 도 맞춰 갱신한다. */
    public WikiDocumentsResponse withoutSchema() {
        List<WikiDocument> filtered = items.stream()
                .filter(d -> !"schema".equals(d.documentKind()))
                .toList();
        return new WikiDocumentsResponse(filtered.size(), filtered);
    }
}
