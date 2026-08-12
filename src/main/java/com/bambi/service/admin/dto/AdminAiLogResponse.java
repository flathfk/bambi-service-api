package com.bambi.service.admin.dto;

import com.bambi.service.admin.AiRequestLog;
import com.bambi.service.admin.AiResponseLog;

import java.time.OffsetDateTime;

/**
 * 관리자 AI 처리 로그 한 줄 — 요청 + 그 결과(응답)를 합친 뷰.
 *
 * DB 에 실제로 있는 것만 담는다. 초기 admin-web 목업엔 task·model 도 있었지만
 * ai_request_logs/ai_response_logs 스키마에 없어 뺐다(endpoint 로 대체, 상태는 응답으로 파생).
 */
public record AdminAiLogResponse(
        Long id,
        OffsetDateTime requestedAt,
        String userEmail, // 요청 사용자 이메일 (user_id 가 없으면 null)
        String endpoint, // 호출한 agent 엔드포인트
        String status, // SUCCESS | FAILED | PROCESSING — 응답 유무·status_code 로 파생
        Integer statusCode, // agent 응답 HTTP 코드. 응답 자체가 없으면(타임아웃·연결 실패) null
        Integer latencyMs // 소요시간(ms). 아직 처리 중이면 null
) {

    public static AdminAiLogResponse of(AiRequestLog request, AiResponseLog response) {
        return new AdminAiLogResponse(
                request.getId(),
                request.getCreatedAt(),
                request.getUserEmailOrNull(),
                request.getEndpoint(),
                AiResponseLog.deriveStatus(response),
                // 목록에도 상태코드를 준다(2026-08-12). 실패 목록에 엔드포인트만 있으면
                // "왜 실패했나"를 알 수 없어 한 건씩 상세를 열어야 했다. 503(agent 연결 실패)과
                // 500(내부 오류)은 대응이 달라 한눈에 갈리는 편이 낫다.
                // null 자체도 정보다 — 응답이 아예 없었다는 뜻(타임아웃·연결 실패).
                response != null ? response.getStatusCode() : null,
                response != null ? response.getLatencyMs() : null);
    }
}
