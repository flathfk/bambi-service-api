package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 사용자 원본을 보존한 개인 LLM Wiki 계정 단위 초기화 결과. */
public record WikiResetResponse(
        @JsonAlias("user_id") String userId,
        @JsonAlias("reset_document_count") int resetDocumentCount,
        @JsonAlias("reset_relation_count") int resetRelationCount,
        @JsonAlias("unsearchable_chunk_count") int unsearchableChunkCount,
        @JsonAlias("retired_wiki_version_count") int retiredWikiVersionCount,
        @JsonAlias("retired_interest_profile_count") int retiredInterestProfileCount,
        @JsonAlias("cancelled_job_count") int cancelledJobCount,
        @JsonAlias("reset_at") String resetAt,
        @JsonAlias("request_id") String requestId) {
}
