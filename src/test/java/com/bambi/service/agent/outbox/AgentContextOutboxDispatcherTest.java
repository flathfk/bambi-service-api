package com.bambi.service.agent.outbox;

import com.bambi.service.agent.AgentGateway;
import com.bambi.service.agent.dto.AgentContextRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link AgentContextOutboxDispatcher}의 성공·실패·지수 backoff 처리를 검증한다. */
class AgentContextOutboxDispatcherTest {

    private final AgentContextOutboxStore store = mock(AgentContextOutboxStore.class);
    private final AgentGateway gateway = mock(AgentGateway.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentContextOutboxDispatcher dispatcher = new AgentContextOutboxDispatcher(
            store, gateway, objectMapper, "worker-test", 30, 5, 20);

    @Test
    void 전송에_성공하면_claim_token으로_published를_기록한다() throws Exception {
        UUID token = UUID.randomUUID();
        AgentContextRequest request = AgentContextRequest.forVersion(1);
        ClaimedAgentContext claimed = new ClaimedAgentContext(
                11L, 7L, objectMapper.writeValueAsString(request), 1, token);
        when(store.claimForUser(eq(7L), eq("worker-test"), eq(Duration.ofSeconds(30))))
                .thenReturn(Optional.of(claimed));
        when(store.markPublished(11L, token)).thenReturn(true);

        dispatcher.dispatchForUser(7L);

        ArgumentCaptor<AgentContextRequest> requestCaptor = ArgumentCaptor.forClass(AgentContextRequest.class);
        verify(gateway).syncUserContext(eq(7L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().contextVersion()).isEqualTo(1);
        verify(store).markPublished(11L, token);
        verify(store, never()).scheduleRetry(anyLong(), any(), any(), any());
    }

    @Test
    void 전송에_실패하면_같은_claim을_backoff_재시도로_돌린다() throws Exception {
        UUID token = UUID.randomUUID();
        ClaimedAgentContext claimed = new ClaimedAgentContext(
                12L, 8L, objectMapper.writeValueAsString(AgentContextRequest.forVersion(2)), 3, token);
        when(store.claimBatch(eq("worker-test"), eq(10), eq(Duration.ofSeconds(30))))
                .thenReturn(List.of(claimed));
        doThrow(new RuntimeException("agent down")).when(gateway).syncUserContext(eq(8L), any());
        when(store.scheduleRetry(eq(12L), eq(token), any(), eq("RuntimeException"))).thenReturn(true);

        int processed = dispatcher.dispatchBatch(10);

        assertThat(processed).isEqualTo(1);
        verify(store).scheduleRetry(12L, token, Duration.ofSeconds(20), "RuntimeException");
        verify(store, never()).markPublished(anyLong(), any());
    }

    @Test
    void backoff는_두배씩_늘고_최댓값에서_멈춘다() {
        assertThat(dispatcher.retryDelay(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(dispatcher.retryDelay(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(dispatcher.retryDelay(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(dispatcher.retryDelay(20)).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void retry_최댓값이_초깃값보다_작으면_설정오류로_기동을_막는다() {
        assertThatThrownBy(() -> new AgentContextOutboxDispatcher(
                store, gateway, objectMapper, "worker-test", 30, 10, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
