package com.bambi.service.mcp.dto;

import java.util.List;

/** 인증 사용자가 발급한 MCP Personal Access Token 목록. */
public record McpApiKeyListResponse(List<McpApiKeyResponse> items) {
}
