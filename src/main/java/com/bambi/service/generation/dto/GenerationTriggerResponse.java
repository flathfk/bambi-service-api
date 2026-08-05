package com.bambi.service.generation.dto;

/**
 * 즉시 리포트 생성 트리거 응답 (POST /api/reports/generate).
 * status=accepted 는 생성 Job 접수(202 성격)를 뜻한다.
 *
 * <p>키 설계(우석 협의): 펜딩 UI 가 쓰는 키는 service 가 발급하는 {@code id} 다 — 항상 보장된다.
 * agent 가 202 를 줘도 body 파싱이 실패하면 agent 식별자({@code agentJobId})는 null 이 될 수 있어
 * 키로 쓸 수 없다. 그래서 id 를 항상-존재 키로, agentJobId 는 참고용(nullable)으로 분리한다.
 * id 는 멱등키에서 파생(deterministic)이라 같은 분 연타는 같은 id 로 모인다(펜딩 중복 방지).
 */
public record GenerationTriggerResponse(String status, String id, String agentJobId) {

    public static GenerationTriggerResponse accepted(String id, String agentJobId) {
        return new GenerationTriggerResponse("accepted", id, agentJobId);
    }
}
