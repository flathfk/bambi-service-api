package com.bambi.service.agent.jobs;

import com.bambi.service.agent.AgentErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/** Agent 내부 Job 상태·결과 API의 RestClient 구현. */
@Component
public class RestClientAgentJobStatusClient implements AgentJobStatusClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientAgentJobStatusClient.class);

    private final RestClient restClient;
    private final String internalPrefix;

    public RestClientAgentJobStatusClient(
            RestClient agentRestClient,
            @Value("${app.agent.internal-prefix}") String internalPrefix) {
        this.restClient = agentRestClient;
        this.internalPrefix = internalPrefix;
    }

    /** Agent의 최대 100건 Batch 상태 조회 계약을 호출한다. */
    @Override
    public AgentJobStatusBatchResponse getStatuses(List<String> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return new AgentJobStatusBatchResponse(List.of(), List.of());
        }
        String path = internalPrefix + "/jobs/statuses";
        try {
            AgentJobStatusBatchResponse response = restClient.post()
                    .uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AgentJobStatusBatchRequest(jobIds))
                    .retrieve()
                    .body(AgentJobStatusBatchResponse.class);
            return response != null ? response : new AgentJobStatusBatchResponse(List.of(), jobIds);
        } catch (RestClientResponseException e) {
            log.warn("[AgentJobStatus] Batch 조회 실패 status={} body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw AgentErrors.unavailable(e, "agent Job 상태 조회 실패");
        } catch (RestClientException e) {
            log.warn("[AgentJobStatus] Batch 조회 연결 실패: {}", e.getMessage());
            throw AgentErrors.connectFailed(e);
        }
    }

    /** URL 수집 완료 후 후속 Wiki Job ID를 얻기 위해 결과를 한 번 조회한다. */
    @Override
    public AgentJobResult getResult(String jobId) {
        String path = internalPrefix + "/jobs/" + jobId + "/result";
        try {
            AgentJobResult response = restClient.get()
                    .uri(path)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .retrieve()
                    .body(AgentJobResult.class);
            if (response == null) {
                throw new RestClientException("agent Job 결과가 비어 있습니다.");
            }
            return response;
        } catch (RestClientResponseException e) {
            log.warn("[AgentJobStatus] 결과 조회 실패 jobId={} status={} body={}", jobId,
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw AgentErrors.unavailable(e, "agent Job 결과 조회 실패");
        } catch (RestClientException e) {
            log.warn("[AgentJobStatus] 결과 조회 연결 실패 jobId={}: {}", jobId, e.getMessage());
            throw AgentErrors.connectFailed(e);
        }
    }
}
