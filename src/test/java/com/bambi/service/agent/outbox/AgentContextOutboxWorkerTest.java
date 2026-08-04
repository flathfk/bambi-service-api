package com.bambi.service.agent.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** {@link AgentContextOutboxWorker}가 폴링 오류를 격리하고 다음 주기를 유지하는지 검증한다. */
class AgentContextOutboxWorkerTest {

    @Test
    void poll은_설정된_batchSize로_dispatch한다() {
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        AgentContextOutboxWorker worker = new AgentContextOutboxWorker(dispatcher, 17);

        worker.poll();

        verify(dispatcher).dispatchBatch(17);
    }

    @Test
    void claim_오류가_나도_스케줄러_호출을_실패시키지_않는다() {
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        doThrow(new RuntimeException("db down")).when(dispatcher).dispatchBatch(17);
        AgentContextOutboxWorker worker = new AgentContextOutboxWorker(dispatcher, 17);

        assertThatCode(worker::poll).doesNotThrowAnyException();
    }

    @Test
    void batchSize가_0이면_설정오류로_기동을_막는다() {
        assertThatThrownBy(() -> new AgentContextOutboxWorker(
                mock(AgentContextOutboxDispatcher.class), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
