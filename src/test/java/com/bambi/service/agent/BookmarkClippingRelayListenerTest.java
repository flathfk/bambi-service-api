package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentClippingRequest;
import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.agent.dto.AgentUrlSourceRequest;
import com.bambi.service.bookmark.BookmarkSavedEvent;
import com.bambi.service.wiki.WikiBuildOperationService;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link BookmarkClippingRelayListener} — 저장 완료 → agent 중계 분기 규칙 검증.
 */
class BookmarkClippingRelayListenerTest {

    /** 클리퍼가 보내는 수준의 페이지 본문(운영 평균 4,213자) — 길이 기준을 넘긴다. */
    private static final String PAGE_CONTENT =
            "본문".repeat(BookmarkClippingRelayListener.MIN_PAGE_CONTENT_LENGTH);

    @Test
    void URL과_페이지_본문이_모두_있으면_클리핑으로_중계한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayClipping(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-1", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "제목", PAGE_CONTENT));

        verify(gateway).relayClipping(eq(1L), any(AgentClippingRequest.class));
        verify(operations).register(eq(1L), argThat(id -> id.startsWith("bookmark-42-")), any(AgentAcceptedJob.class));
        verify(gateway, never()).relayUrlSource(anyLong(), any());
    }

    @Test
    void URL만_있으면_URL_원천으로_중계한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-2", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", null, "  "));

        verify(gateway).relayUrlSource(eq(1L), any(AgentUrlSourceRequest.class));
        verify(operations).register(eq(1L), argThat(id -> id.startsWith("bookmark-42-")), any(AgentAcceptedJob.class));
        verify(gateway, never()).relayClipping(anyLong(), any());
    }

    @Test
    void URL과_짧은_손메모면_클리핑이_아니라_URL_원천으로_보내_agent가_페이지를_읽게_한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-3", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        // 실사례(bookmark 389): 호텔 프로모션 URL + 제목 "도쿄" + 본문 "도쿄 여행".
        listener.onBookmarkSaved(new BookmarkSavedEvent(
                1L, 389L, "https://www.hyatt.com/ko-KR/promo/tokyo", "도쿄", "도쿄 여행"));

        ArgumentCaptor<AgentUrlSourceRequest> sent = ArgumentCaptor.forClass(AgentUrlSourceRequest.class);
        verify(gateway).relayUrlSource(eq(1L), sent.capture());
        verify(gateway, never()).relayClipping(anyLong(), any());
        // 손메모를 버리지 않는다 — URL 내용과 사용자가 적은 키워드가 한 자료에 같이 들어가야 한다.
        assertThat(sent.getValue().memo()).isEqualTo("도쿄 여행");
        assertThat(sent.getValue().url()).isEqualTo("https://www.hyatt.com/ko-KR/promo/tokyo");
    }

    @Test
    void 제목이_손메모에_없으면_제목도_함께_memo로_넘긴다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-4", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "출장 준비", "숙소"));

        ArgumentCaptor<AgentUrlSourceRequest> sent = ArgumentCaptor.forClass(AgentUrlSourceRequest.class);
        verify(gateway).relayUrlSource(eq(1L), sent.capture());
        assertThat(sent.getValue().memo()).isEqualTo("출장 준비 숙소");
    }

    @Test
    void URL만_있고_제목이_있으면_제목이_memo로_간다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-5", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "도쿄", null));

        ArgumentCaptor<AgentUrlSourceRequest> sent = ArgumentCaptor.forClass(AgentUrlSourceRequest.class);
        verify(gateway).relayUrlSource(eq(1L), sent.capture());
        assertThat(sent.getValue().memo()).isEqualTo("도쿄");
    }

    @Test
    void 제목도_본문도_없으면_memo는_비운다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-6", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "  ", null));

        ArgumentCaptor<AgentUrlSourceRequest> sent = ArgumentCaptor.forClass(AgentUrlSourceRequest.class);
        verify(gateway).relayUrlSource(eq(1L), sent.capture());
        assertThat(sent.getValue().memo()).isNull();
    }

    /**
     * 같은 URL 로 키워드를 바꿔 다시 저장하는 실사례(도쿄 → 상하이). 북마크 행은 그대로(id 389)라
     * ID 만으로 키를 만들면 agent 가 "이미 처리한 원천"으로 보고 새 작업을 만들지 않는다.
     */
    @Test
    void 같은_북마크라도_내용이_바뀌면_다른_멱등키로_보내_agent가_다시_읽게_한다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-7", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);
        String url = "https://www.hyatt.com/ko-KR/promo/tokyo";

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 389L, url, "도쿄", "도쿄 여행"));
        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 389L, url, "상하이", "상하이 여행"));

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(operations, times(2)).register(eq(1L), keys.capture(), any(AgentAcceptedJob.class));
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
        assertThat(keys.getAllValues()).allSatisfy(key -> assertThat(key).startsWith("bookmark-389-"));
    }

    @Test
    void 내용이_그대로면_같은_멱등키를_써_중복_처리를_막는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-8", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);
        var event = new BookmarkSavedEvent(1L, 389L, "https://ex.com/a", "도쿄", "도쿄 여행");

        listener.onBookmarkSaved(event);
        listener.onBookmarkSaved(event);

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(operations, times(2)).register(eq(1L), keys.capture(), any(AgentAcceptedJob.class));
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
    }

    /** URL 이 다르면 제목·본문이 같아도 다른 원천이다 — 지문에 URL 도 넣는 이유. */
    @Test
    void URL이_다르면_제목_본문이_같아도_다른_멱등키다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayUrlSource(anyLong(), any())).thenReturn(new AgentAcceptedJob("job-9", "queued"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 7L, "https://ex.com/a", "도쿄", "메모"));
        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 7L, "https://ex.com/b", "도쿄", "메모"));

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(operations, times(2)).register(eq(1L), keys.capture(), any(AgentAcceptedJob.class));
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
    }

    @Test
    void 본문만_있고_URL이_없으면_중계하지_않는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        listener.onBookmarkSaved(new BookmarkSavedEvent(1L, 42L, null, null, "메모만 저장"));

        verifyNoInteractions(gateway);
        verifyNoInteractions(operations);
    }

    @Test
    void 중계가_실패해도_예외를_삼켜_저장을_되돌리지_않는다() {
        AgentGateway gateway = mock(AgentGateway.class);
        WikiBuildOperationService operations = mock(WikiBuildOperationService.class);
        when(gateway.relayClipping(anyLong(), any())).thenThrow(new RuntimeException("agent down"));
        BookmarkClippingRelayListener listener = new BookmarkClippingRelayListener(gateway, operations);

        assertThatCode(() -> listener.onBookmarkSaved(
                new BookmarkSavedEvent(1L, 42L, "https://ex.com/a", "제목", PAGE_CONTENT)))
                .doesNotThrowAnyException();
    }
}
