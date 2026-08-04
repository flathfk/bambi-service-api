package com.bambi.service.agent.outbox;

import com.bambi.service.agent.dto.AgentContextRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** {@link AgentContextOutboxStore}의 payload 보존과 독립 트랜잭션 경계를 검증한다. */
class AgentContextOutboxStoreTest {

    @Test
    void enqueue는_재시도에_쓸_전체_payload를_JSON으로_보존한다() throws Exception {
        AgentContextOutboxRepository repository = mock(AgentContextOutboxRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        AgentContextOutboxStore store = new AgentContextOutboxStore(repository, objectMapper);

        store.enqueue(7L, AgentContextRequest.forVersion(
                3, "1.0.0", java.util.List.of("tech"), java.util.List.of("ai_ml")));

        ArgumentCaptor<AgentContextOutbox> captor = ArgumentCaptor.forClass(AgentContextOutbox.class);
        verify(repository).save(captor.capture());
        AgentContextOutbox saved = captor.getValue();
        JsonNode payload = objectMapper.readTree(saved.getPayload());
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getContextVersion()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(AgentContextOutboxStatus.PENDING);
        assertThat(payload.get("context_version").asInt()).isEqualTo(3);
        assertThat(payload.get("plan").asText()).isEqualTo("free");
        assertThat(payload.get("interest_taxonomy_version").asText()).isEqualTo("1.0.0");
        assertThat(payload.get("selected_category_ids").get(0).asText()).isEqualTo("tech");
        assertThat(payload.get("selected_topic_ids").get(0).asText()).isEqualTo("ai_ml");
        assertThat(payload.get("blocked_interest_ids").isArray()).isTrue();
    }

    @Test
    void afterCommit에서_호출되는_상태변경은_REQUIRES_NEW를_사용한다() throws Exception {
        assertRequiresNew("claimForUser", long.class, String.class, java.time.Duration.class);
        assertRequiresNew("claimBatch", String.class, int.class, java.time.Duration.class);
        assertRequiresNew("markPublished", long.class, java.util.UUID.class);
        assertRequiresNew("scheduleRetry", long.class, java.util.UUID.class,
                java.time.Duration.class, String.class);
    }

    private static void assertRequiresNew(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AgentContextOutboxStore.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
