package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentClippingRequest;
import com.bambi.service.agent.dto.AgentUrlSourceRequest;
import com.bambi.service.bookmark.BookmarkSavedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link BookmarkClippingRelayListener} — 저장 완료 → agent 중계 분기 규칙 검증.
 */
class BookmarkClippingRelayListenerTest {

    @Test
    void URL과_본문이_모두_있으면_클리핑으로_중계한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "제목", "본문"));

        verify(gateway).relayClipping(eq(1L), any(AgentClippingRequest.class));
        verify(gateway, never()).relayUrlSource(anyLong(), any());
    }

    @Test
    void URL만_있으면_URL_원천으로_중계한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", null, "  "));

        verify(gateway).relayUrlSource(eq(1L), any(AgentUrlSourceRequest.class));
        verify(gateway, never()).relayClipping(anyLong(), any());
    }

    @Test
    void 본문만_있고_URL이_없으면_중계하지_않는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, null, null, "메모만 저장"));

        verifyNoInteractions(gateway);
    }

    @Test
    void 중계가_실패해도_예외를_삼켜_저장을_되돌리지_않는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        doThrow(new RuntimeException("agent down")).when(gateway).relayClipping(anyLong(), any());
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway);

        assertThatCode(() -> listener.onBookmarkSaved(
                new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "제목", "본문")))
                .doesNotThrowAnyException();
    }
}
