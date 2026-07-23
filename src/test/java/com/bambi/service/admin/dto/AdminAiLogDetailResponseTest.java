package com.bambi.service.admin.dto;

import com.bambi.service.admin.AiRequestLog;
import com.bambi.service.admin.AiResponseLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AdminAiLogDetailResponse} 매핑 검증 — 상세는 목록에 요청·응답 본문을 더한다.
 */
class AdminAiLogDetailResponseTest {

    private AiRequestLog request() {
        return new AiRequestLog(null, "/internal/v1/users/1/context", "{\"context_version\":1}");
    }

    @Test
    void 응답이_있으면_본문과_상태_상태코드를_함께_담는다() {
        AiResponseLog response = new AiResponseLog(1L, 200, 120, "{\"context_version\":1}");

        AdminAiLogDetailResponse view = AdminAiLogDetailResponse.of(request(), response);

        assertThat(view.status()).isEqualTo("SUCCESS");
        assertThat(view.statusCode()).isEqualTo(200);
        assertThat(view.latencyMs()).isEqualTo(120);
        assertThat(view.requestBody()).contains("context_version");
        assertThat(view.responseBody()).contains("context_version");
    }

    @Test
    void 응답이_없으면_처리중이고_응답측_값은_모두_null_이다() {
        AdminAiLogDetailResponse view = AdminAiLogDetailResponse.of(request(), null);

        assertThat(view.status()).isEqualTo("PROCESSING");
        assertThat(view.statusCode()).isNull();
        assertThat(view.latencyMs()).isNull();
        assertThat(view.respondedAt()).isNull();
        assertThat(view.responseBody()).isNull();
        assertThat(view.requestBody()).contains("context_version"); // 요청 본문은 있다
    }

    @Test
    void status_code가_null이면_연결실패로_FAILED_이고_에러메시지_평문을_본문에_담는다() {
        AiResponseLog response = new AiResponseLog(1L, null, 3000, "connection timeout");

        AdminAiLogDetailResponse view = AdminAiLogDetailResponse.of(request(), response);

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.statusCode()).isNull();
        assertThat(view.responseBody()).isEqualTo("connection timeout");
    }
}
