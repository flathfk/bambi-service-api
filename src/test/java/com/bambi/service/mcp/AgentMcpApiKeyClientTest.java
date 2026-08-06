package com.bambi.service.mcp;

import com.bambi.service.mcp.dto.McpApiKeyCreateRequest;
import com.bambi.service.mcp.dto.McpApiKeyCreateResponse;
import com.bambi.service.mcp.dto.McpApiKeyListResponse;
import com.bambi.service.mcp.dto.McpApiKeyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/** agent-api MCP API Key JSON 계약의 snake_case 변환을 검증한다. */
class AgentMcpApiKeyClientTest {

    private MockRestServiceServer server;
    private AgentMcpApiKeyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://agent.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AgentMcpApiKeyClient(builder.build(), "/internal/v1");
    }

    @Test
    @DisplayName("발급 요청과 응답의 snake_case 필드를 변환한다")
    void createMapsAgentContract() {
        server.expect(once(), requestTo("http://agent.test/internal/v1/users/42/mcp-api-keys"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"name":"Claude","expires_at":1793750400.0}
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":"key-1","name":"Claude","key_prefix":"test",
                          "scopes":["wiki:read"],"status":"active","expires_at":"2026-11-04T00:00:00Z",
                          "last_used_at":null,"created_at":"2026-08-06T00:00:00Z","revoked_at":null,
                          "api_key":"test","token_type":"Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));

        McpApiKeyCreateResponse response = client.create(
                42L,
                new McpApiKeyCreateRequest("Claude", Instant.parse("2026-11-04T00:00:00Z")));

        assertThat(response.keyPrefix()).isEqualTo("test");
        assertThat(response.apiKey()).isEqualTo("test");
        server.verify();
    }

    @Test
    @DisplayName("목록 응답에는 원문 API Key가 없는 관리 정보만 사용한다")
    void listMapsSafeSummary() {
        server.expect(once(), requestTo("http://agent.test/internal/v1/users/42/mcp-api-keys"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"items":[{
                          "id":"key-1","name":"Claude","key_prefix":"test",
                          "scopes":["wiki:read"],"status":"active","expires_at":null,
                          "last_used_at":null,"created_at":"2026-08-06T00:00:00Z","revoked_at":null
                        }]}
                        """, MediaType.APPLICATION_JSON));

        McpApiKeyListResponse response = client.list(42L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).keyPrefix()).isEqualTo("test");
        server.verify();
    }

    @Test
    @DisplayName("폐기는 사용자 경로와 Key ID를 DELETE 요청으로 전달한다")
    void revokeCallsAgentDeleteContract() {
        server.expect(once(), requestTo(
                        "http://agent.test/internal/v1/users/42/mcp-api-keys/11111111-1111-1111-1111-111111111111"))
                .andExpect(method(DELETE))
                .andRespond(withSuccess("""
                        {
                          "id":"11111111-1111-1111-1111-111111111111","name":"Claude",
                          "key_prefix":"test","scopes":["wiki:read"],
                          "status":"revoked","expires_at":null,"last_used_at":null,
                          "created_at":"2026-08-06T00:00:00Z","revoked_at":"2026-08-06T01:00:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        McpApiKeyResponse response = client.revoke(
                42L, "11111111-1111-1111-1111-111111111111");

        assertThat(response.status()).isEqualTo("revoked");
        server.verify();
    }
}
