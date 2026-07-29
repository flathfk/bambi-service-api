package com.bambi.service.wiki;

import com.bambi.service.wiki.dto.WikiDocumentsResponse;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link AgentWikiClient} — agent snake_case 응답을 camelCase 로 읽고 topic→tag 리네임하는지,
 * 없는 사용자(404)를 빈 결과로 정규화하는지 MockRestServiceServer 로 검증한다.
 */
class AgentWikiClientTest {

    private MockRestServiceServer server;
    private AgentWikiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://agent.local");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AgentWikiClient(builder.build(), "/internal/v1");
    }

    @Test
    @DisplayName("관심 조회: topic→tag·interest_id→tagId·document_ids→documentIds 로 매핑한다")
    void getTagsMapsSnakeAndRenamesTopic() {
        String agentBody = """
                {"profile_id":"p1","version":1,"status":"active","calculated_at":"2026-07-22T03:15:18Z",
                 "interests":[{"interest_id":"i1","topic":"원/달러 환율","category":null,"score":1.0,
                   "confidence":0.7,"document_ids":["d1","d2"],"evidence":{"weight":5.0,"reasons":["title"]}}]}
                """;
        server.expect(requestTo("http://agent.local/internal/v1/users/7/interests"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(agentBody, MediaType.APPLICATION_JSON));

        WikiTagsResponse resp = client.getTags(7);

        assertThat(resp.profileId()).isEqualTo("p1");
        assertThat(resp.tags()).hasSize(1);
        assertThat(resp.tags().get(0).tag()).isEqualTo("원/달러 환율"); // topic → tag
        assertThat(resp.tags().get(0).tagId()).isEqualTo("i1");         // interest_id → tagId
        assertThat(resp.tags().get(0).documentIds()).containsExactly("d1", "d2");
        assertThat(resp.tags().get(0).category()).isNull();
    }

    @Test
    @DisplayName("활성 Profile 없는 사용자(agent 404)는 빈 태그 목록으로 정규화한다")
    void getTagsNotFoundReturnsEmpty() {
        server.expect(requestTo("http://agent.local/internal/v1/users/7/interests"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"code\":\"INTEREST_PROFILE_NOT_FOUND\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        WikiTagsResponse resp = client.getTags(7);

        assertThat(resp.tags()).isEmpty();
        assertThat(resp.status()).isEqualTo("empty");
    }

    @Test
    @DisplayName("문서 조회: snake_case 를 읽어 그대로 담는다(schema 제외는 서비스 몫)")
    void getDocumentsReadsSnake() {
        String agentBody = """
                {"total":2,"items":[
                  {"document_id":"c1","document_kind":"concept","title":"개인 지식 그래프","summary":"요약",
                   "domain":"other","source_count":1,"updated_at":"2026-07-22T03:15:18Z"},
                  {"document_id":"s1","document_kind":"schema","title":"Schema","summary":null,
                   "domain":null,"source_count":0,"updated_at":"2026-07-22T03:15:18Z"}]}
                """;
        server.expect(requestTo("http://agent.local/internal/v1/users/7/wiki/documents"))
                .andRespond(withSuccess(agentBody, MediaType.APPLICATION_JSON));

        WikiDocumentsResponse resp = client.getDocuments(7);

        assertThat(resp.total()).isEqualTo(2);
        assertThat(resp.items()).hasSize(2);
        assertThat(resp.items().get(0).documentId()).isEqualTo("c1");
        assertThat(resp.items().get(0).documentKind()).isEqualTo("concept");
    }
}
