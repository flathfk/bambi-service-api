package com.bambi.service.agent;

import com.bambi.service.admin.AiRequestLogRepository;
import com.bambi.service.admin.AiResponseLog;
import com.bambi.service.admin.AiResponseLogRepository;
import com.bambi.service.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** {@link AgentCallLogger}가 JSONB 응답 본문을 항상 유효한 JSON으로 저장하는지 검증한다. */
class AgentCallLoggerTest {

    private final AiResponseLogRepository responseRepository = mock(AiResponseLogRepository.class);
    private final AgentCallLogger logger = new AgentCallLogger(
            mock(AiRequestLogRepository.class), responseRepository,
            mock(UserRepository.class), new ObjectMapper());

    @Test
    void agent_JSON_응답은_그대로_보존한다() {
        logger.logResponse(1L, 503, 10, "{\"code\":\"AGENT_UNAVAILABLE\"}");

        AiResponseLog saved = captureSavedResponse();
        assertThat(saved.getResponseBody()).isEqualTo("{\"code\":\"AGENT_UNAVAILABLE\"}");
    }

    @Test
    void 연결_오류_일반문자열은_JSON_string_literal로_변환한다() {
        logger.logResponse(1L, null, 10, "I/O error on PUT request");

        AiResponseLog saved = captureSavedResponse();
        assertThat(saved.getResponseBody()).isEqualTo("\"I/O error on PUT request\"");
    }

    private AiResponseLog captureSavedResponse() {
        ArgumentCaptor<AiResponseLog> captor = ArgumentCaptor.forClass(AiResponseLog.class);
        verify(responseRepository).save(captor.capture());
        return captor.getValue();
    }
}
