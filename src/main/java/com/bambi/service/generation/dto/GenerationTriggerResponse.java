package com.bambi.service.generation.dto;

/**
 * 즉시 리포트 생성 트리거 응답 (POST /api/reports/generate).
 * status=accepted 는 생성 Job 접수(202 성격)를 뜻한다. jobId 는 진행상태 조회용(펜딩 UI).
 * <p>TODO(송우 답): jobId 노출 필요 여부 확정. 필요 시 GenerationClient 가 job_id 를 반환하도록 확장.
 */
public record GenerationTriggerResponse(String status, String jobId) {

    public static GenerationTriggerResponse accepted(String jobId) {
        return new GenerationTriggerResponse("accepted", jobId);
    }
}
