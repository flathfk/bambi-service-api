package com.bambi.service.wiki.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WikiDocumentsResponse#withoutSchema()} — 내부 schema 문서 제외 + total 재계산 검증.
 */
class WikiDocumentsResponseTest {

    private WikiDocument doc(String id, String kind) {
        return new WikiDocument(id, kind, "제목", "요약", "other", 1, "2026-07-22T03:15:18Z");
    }

    @Test
    void schema_문서를_제외하고_total을_다시_센다() {
        WikiDocumentsResponse raw = new WikiDocumentsResponse(
                3, List.of(doc("c1", "concept"), doc("s1", "schema"), doc("d1", "document")));

        WikiDocumentsResponse filtered = raw.withoutSchema();

        assertThat(filtered.items()).hasSize(2);
        assertThat(filtered.total()).isEqualTo(2);
        assertThat(filtered.items()).noneMatch(d -> "schema".equals(d.documentKind()));
    }
}
