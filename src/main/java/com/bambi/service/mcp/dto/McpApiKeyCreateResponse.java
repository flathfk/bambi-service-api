package com.bambi.service.mcp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

/** 발급 직후 한 번만 원문 API Key를 포함하는 응답. */
public record McpApiKeyCreateResponse(
        String id,
        String name,
        @JsonAlias("key_prefix") String keyPrefix,
        List<String> scopes,
        String status,
        @JsonAlias("expires_at") Instant expiresAt,
        @JsonAlias("last_used_at") Instant lastUsedAt,
        @JsonAlias("created_at") Instant createdAt,
        @JsonAlias("revoked_at") Instant revokedAt,
        @JsonAlias("api_key") String apiKey,
        @JsonAlias("token_type") String tokenType) {
}
