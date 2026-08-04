package com.bambi.service.agent;

import com.bambi.service.agent.outbox.AgentContextOutboxDispatcher;
import com.bambi.service.onboarding.OnboardingSelectionsChangedEvent;
import com.bambi.service.user.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link AgentContextSyncListener} 단위 테스트 — 가입 이벤트 처리 규약 검증.
 * BEFORE_COMMIT에는 적재 실패를 전파하고, AFTER_COMMIT 전송 실패는 삼키는지 검증한다.
 */
class AgentContextSyncListenerTest {

    @Test
    void 커밋_전에_outbox를_적재한다() {
        AgentContextSyncService syncService = mock(AgentContextSyncService.class);
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        AgentContextSyncListener listener = new AgentContextSyncListener(syncService, dispatcher);

        listener.enqueueBeforeCommit(new UserRegisteredEvent(42L));

        verify(syncService).enqueueUserContext(eq(42L));
    }

    @Test
    void outbox_적재가_실패하면_예외를_전파해_가입도_롤백시킨다() {
        AgentContextSyncService syncService = mock(AgentContextSyncService.class);
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        doThrow(new RuntimeException("db down")).when(syncService).enqueueUserContext(anyLong());
        AgentContextSyncListener listener = new AgentContextSyncListener(syncService, dispatcher);

        assertThatThrownBy(() -> listener.enqueueBeforeCommit(new UserRegisteredEvent(7L)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void 커밋_뒤_즉시_전송을_시도한다() {
        AgentContextSyncService syncService = mock(AgentContextSyncService.class);
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        AgentContextSyncListener listener = new AgentContextSyncListener(syncService, dispatcher);

        listener.dispatchAfterCommit(new UserRegisteredEvent(42L));

        verify(dispatcher).dispatchForUser(eq(42L));
    }

    @Test
    void 커밋_뒤_전송이_실패해도_예외를_삼켜_워커_재시도를_허용한다() {
        AgentContextSyncService syncService = mock(AgentContextSyncService.class);
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        doThrow(new RuntimeException("agent down")).when(dispatcher).dispatchForUser(anyLong());
        AgentContextSyncListener listener = new AgentContextSyncListener(syncService, dispatcher);

        assertThatCode(() -> listener.dispatchAfterCommit(new UserRegisteredEvent(7L)))
                .doesNotThrowAnyException();

        verify(dispatcher).dispatchForUser(eq(7L));
    }

    @Test
    void 온보딩_선택도_커밋_전_적재하고_커밋_뒤_즉시_전송한다() {
        AgentContextSyncService syncService = mock(AgentContextSyncService.class);
        AgentContextOutboxDispatcher dispatcher = mock(AgentContextOutboxDispatcher.class);
        AgentContextSyncListener listener = new AgentContextSyncListener(syncService, dispatcher);
        OnboardingSelectionsChangedEvent event = new OnboardingSelectionsChangedEvent(42L);

        listener.enqueueOnboardingBeforeCommit(event);
        listener.dispatchOnboardingAfterCommit(event);

        verify(syncService).enqueueUserContext(eq(42L));
        verify(dispatcher).dispatchForUser(eq(42L));
    }
}
