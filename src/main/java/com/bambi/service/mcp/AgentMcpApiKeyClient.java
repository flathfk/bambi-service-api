package com.bambi.service.mcp;

import com.bambi.service.agent.AgentErrors;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.mcp.dto.McpApiKeyCreateRequest;
import com.bambi.service.mcp.dto.McpApiKeyCreateResponse;
import com.bambi.service.mcp.dto.McpApiKeyListResponse;
import com.bambi.service.mcp.dto.McpApiKeyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** agent-api의 사용자별 MCP API Key 수명 주기 경로를 호출한다. */
@Component
public class AgentMcpApiKeyClient {

    private final RestClient restClient;
    private final String internalPrefix;

    public AgentMcpApiKeyClient(RestClient agentRestClient,
                                @Value("${app.agent.internal-prefix}") String internalPrefix) {
        this.restClient = agentRestClient;
        this.internalPrefix = internalPrefix;
    }

    /** 사용자 Wiki 읽기 전용 MCP API Key를 발급한다. */
    public McpApiKeyCreateResponse create(long userId, McpApiKeyCreateRequest request) {
        try {
            return restClient.post()
                    .uri(internalPrefix + "/users/{userId}/mcp-api-keys", userId)
                    .body(request)
                    .retrieve()
                    .body(McpApiKeyCreateResponse.class);
        } catch (RestClientResponseException e) {
            throw mapAgentError(e, "agent MCP API Key 발급 실패");
        } catch (RestClientException e) {
            throw AgentErrors.connectFailed(e);
        }
    }

    /** 원문과 Hash가 제거된 사용자 MCP API Key 목록을 조회한다. */
    public McpApiKeyListResponse list(long userId) {
        try {
            return restClient.get()
                    .uri(internalPrefix + "/users/{userId}/mcp-api-keys", userId)
                    .retrieve()
                    .body(McpApiKeyListResponse.class);
        } catch (RestClientResponseException e) {
            throw mapAgentError(e, "agent MCP API Key 목록 조회 실패");
        } catch (RestClientException e) {
            throw AgentErrors.connectFailed(e);
        }
    }

    /** 사용자 소유 MCP API Key를 영구 폐기한다. */
    public McpApiKeyResponse revoke(long userId, String keyId) {
        try {
            return restClient.delete()
                    .uri(internalPrefix + "/users/{userId}/mcp-api-keys/{keyId}", userId, keyId)
                    .retrieve()
                    .body(McpApiKeyResponse.class);
        } catch (RestClientResponseException e) {
            throw mapAgentError(e, "agent MCP API Key 폐기 실패");
        } catch (RestClientException e) {
            throw AgentErrors.connectFailed(e);
        }
    }

    private RuntimeException mapAgentError(RestClientResponseException error, String context) {
        if (error.getStatusCode().value() == 404) {
            return new ApiException(ErrorCode.NOT_FOUND, "MCP API Key를 찾을 수 없습니다.");
        }
        if (error.getStatusCode().value() == 422) {
            return new ApiException(ErrorCode.VALIDATION_ERROR, "MCP API Key 설정이 올바르지 않습니다.");
        }
        return AgentErrors.unavailable(error, context);
    }
}
