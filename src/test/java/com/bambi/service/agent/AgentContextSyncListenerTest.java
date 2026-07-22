package com.bambi.service.agent;

import com.bambi.service.user.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link AgentContextSyncListener} 단위 테스트 — 가입 이벤트 처리 규약 검증.
 */
class AgentContextSyncListenerTest {

    @Test
    void 가입_이벤트를_받으면_컨텍스트_동기화를_1회_호출한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        AgentContextSyncListener listener = new AgentContextSyncListener(gateway);

        listener.onUserRegistered(new UserRegisteredEvent(42L));

        verify(gateway).syncUserContext(eq(42L), any());
    }

    @Test
    void agent_동기화가_실패해도_예외를_삼켜_가입을_막지_않는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        doThrow(new RuntimeException("agent down")).when(gateway).syncUserContext(anyLong(), any());
        AgentContextSyncListener listener = new AgentContextSyncListener(gateway);

        assertThatCode(() -> listener.onUserRegistered(new UserRegisteredEvent(7L)))
                .doesNotThrowAnyException();

        verify(gateway).syncUserContext(anyLong(), any());
    }
}
