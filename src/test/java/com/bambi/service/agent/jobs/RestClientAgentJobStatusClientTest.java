package com.bambi.service.agent.jobs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Agent Job 상태 Batch와 결과 조회 HTTP 계약을 검증한다. */
class RestClientAgentJobStatusClientTest {

    private MockRestServiceServer server;
    private RestClientAgentJobStatusClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://agent.local");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientAgentJobStatusClient(builder.build(), "/internal/v1");
    }

    @Test
    void 활성_Job을_Batch로_조회한다() {
        server.expect(requestTo("http://agent.local/internal/v1/jobs/statuses"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"items":[{"job_id":"job-1","job_type":"personal_wiki_build",
                        "status":"running","progress":5,"error_code":null}],"missing_job_ids":["job-2"]}
                        """, MediaType.APPLICATION_JSON));

        AgentJobStatusBatchResponse response = client.getStatuses(List.of("job-1", "job-2"));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo("running");
        assertThat(response.missingJobIds()).containsExactly("job-2");
        server.verify();
    }

    @Test
    void URL_Job_완료_결과에서_후속_Wiki_Job을_읽는다() {
        server.expect(requestTo("http://agent.local/internal/v1/jobs/job-url/result"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"job_id":"job-url","status":"completed",
                        "result":{"wiki_build_job_id":"job-wiki"}}
                        """, MediaType.APPLICATION_JSON));

        AgentJobResult result = client.getResult("job-url");

        assertThat(result.stringValue("wiki_build_job_id")).isEqualTo("job-wiki");
        server.verify();
    }
}
