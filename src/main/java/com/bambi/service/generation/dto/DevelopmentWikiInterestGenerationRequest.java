package com.bambi.service.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 개발용 Wiki 관심사 리포트 즉시 생성 요청. */
public record DevelopmentWikiInterestGenerationRequest(
        @NotBlank @Size(max = 64) String tagId) {

    /** 비교와 Agent 요청에 사용할 공백 제거 식별자. */
    public String normalizedTagId() {
        return tagId.strip();
    }
}
