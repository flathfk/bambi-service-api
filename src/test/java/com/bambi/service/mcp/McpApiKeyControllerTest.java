package com.bambi.service.mcp;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.mcp.dto.McpApiKeyCreateRequest;
import com.bambi.service.mcp.dto.McpApiKeyCreateResponse;
import com.bambi.service.mcp.dto.McpApiKeyListResponse;
import com.bambi.service.mcp.dto.McpApiKeyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** MCP API Key Controller가 요청 입력 대신 인증 주체로 사용자 범위를 강제하는지 검증한다. */
class McpApiKeyControllerTest {

    private final McpApiKeyService service = mock(McpApiKeyService.class);
    private final McpApiKeyController controller = new McpApiKeyController(service);
    private final AuthPrincipal principal = new AuthPrincipal(42L, "mcp@bambi.test");

    @Test
    @DisplayName("Key 발급은 인증 주체의 사용자 ID만 사용한다")
    void createUsesPrincipalUserId() {
        McpApiKeyCreateRequest request = new McpApiKeyCreateRequest("Claude", null);
        McpApiKeyCreateResponse created = new McpApiKeyCreateResponse(
                "key-1", "Claude", "test", List.of("wiki:read"),
                "active", null, null, Instant.parse("2026-08-06T00:00:00Z"), null,
                "test", "Bearer");
        when(service.create(42L, request)).thenReturn(created);

        ApiResponse<McpApiKeyCreateResponse> response = controller.create(principal, request);

        assertThat(response.getData()).isSameAs(created);
        verify(service).create(42L, request);
    }

    @Test
    @DisplayName("Key 목록과 폐기는 인증 주체의 사용자 ID만 사용한다")
    void listAndRevokeUsePrincipalUserId() {
        McpApiKeyListResponse list = new McpApiKeyListResponse(List.of());
        McpApiKeyResponse revoked = new McpApiKeyResponse(
                "key-1", "Claude", "test", List.of("wiki:read"),
                "revoked", null, null, Instant.parse("2026-08-06T00:00:00Z"),
                Instant.parse("2026-08-06T01:00:00Z"));
        when(service.list(42L)).thenReturn(list);
        when(service.revoke(42L, "key-1")).thenReturn(revoked);

        assertThat(controller.list(principal).getData()).isSameAs(list);
        assertThat(controller.revoke(principal, "key-1").getData()).isSameAs(revoked);
        verify(service).list(42L);
        verify(service).revoke(42L, "key-1");
    }
}
