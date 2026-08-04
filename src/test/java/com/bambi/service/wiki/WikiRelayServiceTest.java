package com.bambi.service.wiki;

import com.bambi.service.wiki.dto.WikiTopNodesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WikiRelayService} — 연결 상위 노드 limit 을 agent 계약 범위(1~100)로 잘라 맞추는지 검증.
 */
class WikiRelayServiceTest {

    private final AgentWikiClient wikiClient = mock(AgentWikiClient.class);
    private final WikiRelayService service = new WikiRelayService(wikiClient);

    @Test
    @DisplayName("top-nodes: 범위 안 limit 은 그대로 전달한다")
    void topNodesPassesInRangeLimit() {
        when(wikiClient.getTopNodes(eq(1L), anyInt())).thenReturn(new WikiTopNodesResponse(0, List.of()));

        service.topNodes(1L, 10);

        verify(wikiClient).getTopNodes(1L, 10);
    }

    @Test
    @DisplayName("top-nodes: 0·음수 limit 은 1 로 올려 맞춘다")
    void topNodesClampsLowerBound() {
        when(wikiClient.getTopNodes(eq(1L), anyInt())).thenReturn(new WikiTopNodesResponse(0, List.of()));

        service.topNodes(1L, 0);
        service.topNodes(1L, -5);

        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(wikiClient, org.mockito.Mockito.times(2)).getTopNodes(eq(1L), limit.capture());
        assertThat(limit.getAllValues()).containsExactly(1, 1);
    }

    @Test
    @DisplayName("top-nodes: 100 초과 limit 은 100 으로 내려 맞춘다")
    void topNodesClampsUpperBound() {
        when(wikiClient.getTopNodes(eq(1L), anyInt())).thenReturn(new WikiTopNodesResponse(0, List.of()));

        service.topNodes(1L, 500);

        verify(wikiClient).getTopNodes(1L, 100);
    }
}
