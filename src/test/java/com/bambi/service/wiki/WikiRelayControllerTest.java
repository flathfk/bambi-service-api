package com.bambi.service.wiki;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.wiki.dto.WikiDocumentDetailResponse;
import com.bambi.service.wiki.dto.WikiGraphResponse;
import com.bambi.service.wiki.dto.WikiGraphStats;
import com.bambi.service.wiki.dto.WikiResetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link WikiRelayController}가 URL 입력 대신 인증 주체로 사용자 범위를 강제하는지 검증한다. */
class WikiRelayControllerTest {

    private final WikiRelayService service = mock(WikiRelayService.class);
    private final WikiRelayController controller = new WikiRelayController(service);
    private final AuthPrincipal principal = new AuthPrincipal(42L, "wiki@bambi.test");

    @Test
    @DisplayName("Graph 조회는 인증 주체의 사용자 ID만 사용한다")
    void graphUsesPrincipalUserId() {
        WikiGraphResponse graph = new WikiGraphResponse(
                1, "2026-08-05T00:00:00Z", new WikiGraphStats(0, 0, 0, 0, 0), List.of(), List.of());
        when(service.graph(42L)).thenReturn(graph);

        ApiResponse<WikiGraphResponse> response = controller.graph(principal);

        assertThat(response.getData()).isSameAs(graph);
        verify(service).graph(42L);
    }

    @Test
    @DisplayName("문서 상세 조회는 인증 주체와 선택한 문서 ID를 함께 전달한다")
    void documentUsesPrincipalUserId() {
        WikiDocumentDetailResponse detail = new WikiDocumentDetailResponse(
                "node-1", "version-1", "entity", "node", "entities/node.md", "other",
                "Node", "요약", 1, 0, "2026-08-05T00:00:00Z", "## Node", List.of(), List.of());
        when(service.document(42L, "node-1")).thenReturn(detail);

        ApiResponse<WikiDocumentDetailResponse> response = controller.document(principal, "node-1");

        assertThat(response.getData()).isSameAs(detail);
        verify(service).document(42L, "node-1");
    }

    @Test
    @DisplayName("Wiki 초기화는 요청 경로의 사용자 ID 없이 인증 주체만 사용한다")
    void resetUsesPrincipalUserId() {
        WikiResetResponse reset = new WikiResetResponse(
                "42", 1, 2, 3, 1, 1, 0,
                "2026-08-10T00:00:00Z", "request-1");
        when(service.reset(42L)).thenReturn(reset);

        ApiResponse<WikiResetResponse> response = controller.reset(principal);

        assertThat(response.getData()).isSameAs(reset);
        verify(service).reset(42L);
    }
}
