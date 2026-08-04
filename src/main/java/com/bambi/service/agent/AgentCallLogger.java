package com.bambi.service.agent;

import com.bambi.service.admin.AiRequestLog;
import com.bambi.service.admin.AiRequestLogRepository;
import com.bambi.service.admin.AiResponseLog;
import com.bambi.service.admin.AiResponseLogRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * agent 호출 1건을 service.ai_request_logs / ai_response_logs 에 적재한다.
 *
 * <p>요청/응답을 각각 REQUIRES_NEW 트랜잭션으로 커밋한다 — agent 호출이 실패해
 * 상위 로직이 롤백/예외로 끝나도 "무엇을 호출했고 어떻게 실패했는지"는 로그로 남긴다.
 */
@Component
public class AgentCallLogger {

    private final AiRequestLogRepository requestLogRepository;
    private final AiResponseLogRepository responseLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AgentCallLogger(AiRequestLogRepository requestLogRepository,
                           AiResponseLogRepository responseLogRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.requestLogRepository = requestLogRepository;
        this.responseLogRepository = responseLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 호출 직전 요청 로그를 남기고 그 id 를 돌려준다. 응답 로그와 이 id 로 이어진다.
     *
     * @param userId      요청 주체 사용자 id (사용자와 무관한 호출이면 null)
     * @param endpoint    호출한 agent 경로
     * @param requestBody 요청 본문 JSON 문자열 (없으면 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long logRequest(Long userId, String endpoint, String requestBody) {
        // getReferenceById: 프록시만 얻어 FK(user_id)만 채운다 — User 를 실제로 로드하지 않는다.
        User user = (userId == null) ? null : userRepository.getReferenceById(userId);
        AiRequestLog saved = requestLogRepository.save(new AiRequestLog(user, endpoint, requestBody));
        return saved.getId();
    }

    /**
     * 호출 결과(응답/실패)를 남긴다.
     *
     * @param requestId    {@link #logRequest} 가 돌려준 id (null 이면 기록 건너뜀)
     * @param statusCode   HTTP 상태코드 (연결 실패 등으로 없으면 null)
     * @param latencyMs    소요 시간(ms)
     * @param responseBody 응답 본문 또는 에러 메시지 (없으면 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logResponse(Long requestId, Integer statusCode, Integer latencyMs, String responseBody) {
        if (requestId == null) {
            return;
        }
        responseLogRepository.save(new AiResponseLog(
                requestId, statusCode, latencyMs, normalizeJson(responseBody)));
    }

    /** JSONB 컬럼에 연결 오류 같은 일반 문자열도 안전하게 저장할 수 있도록 JSON 문자열로 감싼다. */
    private String normalizeJson(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        try {
            if (objectMapper.readTree(responseBody) != null) {
                return responseBody;
            }
        } catch (JsonProcessingException ignored) {
            // 일반 문자열은 아래에서 JSON string literal로 직렬화한다.
        }
        try {
            return objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Agent 응답 로그 JSON 직렬화 실패", e);
        }
    }
}
