package com.bambi.service.generation.dto;

import jakarta.validation.constraints.Size;

/**
 * 즉시 리포트 생성 요청 본문 (POST /api/reports/generate). 전부 선택값이다.
 * <p>topic 을 주면 그 주제로 생성한다(프론트가 위키 상위 관심사를 골라 넘기는 경로). 비우면 서버 기본값을 쓴다.
 * agent 계약상 topic 은 1~500자.
 */
public record GenerationTriggerRequest(
        @Size(max = 500) String topic) {
}
