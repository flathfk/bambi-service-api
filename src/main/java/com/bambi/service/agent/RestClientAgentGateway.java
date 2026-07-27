package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentClippingRequest;
import com.bambi.service.agent.dto.AgentContextRequest;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * {@link AgentGateway} 의 실제 구현 — RestClient 로 agent-api 를 HTTP 호출한다.
 * 호출 전후를 {@link AgentCallLogger} 로 남기고, agent 오류를 팀 공통 에러로 변환한다.
 */
@Component
public class RestClientAgentGateway implements AgentGateway {

    private static final Logger log = LoggerFactory.getLogger(RestClientAgentGateway.class);

    private final RestClient restClient;
    private final String internalPrefix;
    private final AgentCallLogger callLogger;
    private final ObjectMapper objectMapper;

    public RestClientAgentGateway(
            RestClient agentRestClient,
            @Value("${app.agent.internal-prefix}") String internalPrefix,
            AgentCallLogger callLogger,
            ObjectMapper objectMapper) {
        this.restClient = agentRestClient;
        this.internalPrefix = internalPrefix;
        this.callLogger = callLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public void syncUserContext(long userId, AgentContextRequest request) {
        String path = internalPrefix + "/users/" + userId + "/context";
        String requestBody = toJson(request);
        Long reqLogId = callLogger.logRequest(userId, path, requestBody);
        long startNanos = System.nanoTime();

        try {
            ResponseEntity<String> resp = restClient.put()
                    .uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            callLogger.logResponse(reqLogId, resp.getStatusCode().value(), elapsedMs(startNanos), resp.getBody());

        } catch (RestClientResponseException e) {
            // agent 가 응답은 줬으나 4xx/5xx
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            callLogger.logResponse(reqLogId, status, elapsedMs(startNanos), body);

            // 이미 최신 버전이면(STALE_CONTEXT_VERSION) 오류가 아니라 "이미 반영됨" → 조용히 통과
            if (status == 409 && body != null && body.contains("STALE_CONTEXT_VERSION")) {
                log.info("[AgentGateway] context 이미 최신(userId={}) — STALE 무시", userId);
                return;
            }
            throw new ApiException(ErrorCode.AGENT_UNAVAILABLE,
                    "agent 컨텍스트 동기화 실패 (status=" + status + ")");

        } catch (RestClientException e) {
            // 연결 실패/타임아웃 등 (응답 자체가 없음)
            callLogger.logResponse(reqLogId, null, elapsedMs(startNanos), e.getMessage());
            throw new ApiException(ErrorCode.AGENT_UNAVAILABLE, "agent 연결 실패: " + e.getMessage());
        }
    }

    @Override
    public void relayClipping(long userId, AgentClippingRequest request) {
        String path = internalPrefix + "/users/" + userId + "/wiki-sources/clippings";
        String requestBody = toJson(request);
        Long reqLogId = callLogger.logRequest(userId, path, requestBody);
        long startNanos = System.nanoTime();

        try {
            ResponseEntity<String> resp = restClient.post()
                    .uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            // 202 Accepted 가 정상 — Job 접수만 확인한다(결과는 service-worker Pull).
            callLogger.logResponse(reqLogId, resp.getStatusCode().value(), elapsedMs(startNanos), resp.getBody());

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            callLogger.logResponse(reqLogId, status, elapsedMs(startNanos), e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.AGENT_UNAVAILABLE,
                    "agent 클리핑 중계 실패 (status=" + status + ")");

        } catch (RestClientException e) {
            callLogger.logResponse(reqLogId, null, elapsedMs(startNanos), e.getMessage());
            throw new ApiException(ErrorCode.AGENT_UNAVAILABLE, "agent 연결 실패: " + e.getMessage());
        }
    }

    /** 로그 적재용 요청 본문 직렬화. 실패해도 호출은 계속하고 본문만 비운다. */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[AgentGateway] 요청 본문 직렬화 실패 — 로그 본문 생략", e);
            return null;
        }
    }

    private int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000L);
    }
}
