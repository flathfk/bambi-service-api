package com.bambi.service.mcp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

/** 원문과 Hash를 제외한 MCP Personal Access Token 관리 정보. */
public record McpApiKeyResponse(
        String id,
        String name,
        @JsonAlias("key_prefix") String keyPrefix,
        List<String> scopes,
        String status,
        @JsonAlias("expires_at") Instant expiresAt,
        @JsonAlias("last_used_at") Instant lastUsedAt,
        @JsonAlias("created_at") Instant createdAt,
        @JsonAlias("revoked_at") Instant revokedAt) {
}
