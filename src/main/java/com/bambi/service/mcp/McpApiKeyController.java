package com.bambi.service.mcp;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.mcp.dto.McpApiKeyCreateRequest;
import com.bambi.service.mcp.dto.McpApiKeyCreateResponse;
import com.bambi.service.mcp.dto.McpApiKeyListResponse;
import com.bambi.service.mcp.dto.McpApiKeyResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 MCP Personal Access Token을 발급·조회·폐기하는 API. */
@RestController
@RequestMapping("/api/mcp/keys")
public class McpApiKeyController {

    private final McpApiKeyService service;

    public McpApiKeyController(McpApiKeyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<McpApiKeyCreateResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody McpApiKeyCreateRequest request) {
        return ApiResponse.ok(service.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<McpApiKeyListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.list(principal.id()));
    }

    @DeleteMapping("/{keyId}")
    public ApiResponse<McpApiKeyResponse> revoke(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String keyId) {
        return ApiResponse.ok(service.revoke(principal.id(), keyId));
    }
}
