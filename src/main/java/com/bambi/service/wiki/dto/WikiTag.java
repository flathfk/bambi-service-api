package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.Map;

/**
 * 자동 추출 태그 하나 (agent {@code InterestItem} → tag 리네임).
 * score 는 0~1 상대 강도(음수 없음), confidence 는 휴리스틱 신뢰도, documentIds 는 근거 위키 문서.
 */
public record WikiTag(
        @JsonAlias("interest_id") String tagId,
        @JsonAlias("topic") String tag,
        String category,
        double score,
        double confidence,
        @JsonAlias("document_ids") List<String> documentIds,
        Map<String, Object> evidence) {
}
