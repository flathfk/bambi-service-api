package com.bambi.service.admin.dto;

import com.bambi.service.admin.AiRequestLog;
import com.bambi.service.admin.AiResponseLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AdminAiLogResponse} 상태 파생 검증.
 * 핵심: "응답 기록은 있는데 status_code 가 null"(agent 연결 실패)이 PROCESSING 이 아니라 FAILED 여야 한다.
 */
class AdminAiLogResponseTest {

    private AiRequestLog request() {
        return new AiRequestLog(null, "/internal/v1/users/1/context", "{}");
    }

    @Test
    void 응답_기록이_없으면_PROCESSING() {
        AdminAiLogResponse view = AdminAiLogResponse.of(request(), null);
        assertThat(view.status()).isEqualTo("PROCESSING");
    }

    @Test
    void 응답_기록은_있는데_status_code_가_null이면_FAILED() {
        AiResponseLog response = new AiResponseLog(1L, null, 3000, "connection timeout");
        AdminAiLogResponse view = AdminAiLogResponse.of(request(), response);
        assertThat(view.status()).isEqualTo("FAILED"); // 이전엔 PROCESSING(버그)
    }

    @Test
    void status_code_2xx면_SUCCESS() {
        AiResponseLog response = new AiResponseLog(1L, 200, 120, "{}");
        assertThat(AdminAiLogResponse.of(request(), response).status()).isEqualTo("SUCCESS");
    }

    @Test
    void status_code_비2xx면_FAILED() {
        AiResponseLog response = new AiResponseLog(1L, 503, 50, "{\"code\":\"AGENT_UNAVAILABLE\"}");
        assertThat(AdminAiLogResponse.of(request(), response).status()).isEqualTo("FAILED");
    }
}
