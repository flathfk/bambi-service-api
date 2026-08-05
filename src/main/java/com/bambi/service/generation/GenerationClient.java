package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;

/**
 * Service → agent 콘텐츠 생성 요청 경계 (§3.4).
 * P0(이번 주)는 {@link MockGenerationClient} 로 요청만 로깅하고,
 * P1 에서 실제 agent-api(POST /internal/v1/users/{id}/generations) 호출 구현으로 교체한다.
 * (실제 HTTP 배선은 AgentGateway 계열과 함께 소라 협의 — 스케줄러는 이 인터페이스만 안다)
 */
public interface GenerationClient {

    /** 사용자의 콘텐츠 생성 Job 을 요청한다(202 비동기). agent 가 준 job_id 를 반환한다(없으면 null). */
    String requestGeneration(long userId, GenerationRequest request);
}
