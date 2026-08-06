package com.bambi.service.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** MCP Personal Access Token 발급 요청. Scope는 agent-api에서 wiki:read로 고정한다. */
public record McpApiKeyCreateRequest(
        @NotBlank @Size(max = 64) String name,
        @Future @JsonProperty("expires_at") Instant expiresAt) {
}
