package com.bambi.service.mcp;

import com.bambi.service.mcp.dto.McpApiKeyCreateRequest;
import com.bambi.service.mcp.dto.McpApiKeyCreateResponse;
import com.bambi.service.mcp.dto.McpApiKeyListResponse;
import com.bambi.service.mcp.dto.McpApiKeyResponse;
import org.springframework.stereotype.Service;

/** 인증 사용자 범위의 MCP Personal Access Token 관리를 조정한다. */
@Service
public class McpApiKeyService {

    private final AgentMcpApiKeyClient client;

    public McpApiKeyService(AgentMcpApiKeyClient client) {
        this.client = client;
    }

    public McpApiKeyCreateResponse create(long userId, McpApiKeyCreateRequest request) {
        return client.create(userId, request);
    }

    public McpApiKeyListResponse list(long userId) {
        return client.list(userId);
    }

    public McpApiKeyResponse revoke(long userId, String keyId) {
        return client.revoke(userId, keyId);
    }
}
