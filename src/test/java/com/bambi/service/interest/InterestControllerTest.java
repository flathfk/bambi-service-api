package com.bambi.service.interest;

import com.bambi.service.agent.AgentContextSyncService;
import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** {@link InterestController}의 Agent 컨텍스트 동기화 진입점을 검증한다. */
class InterestControllerTest {

    @Test
    void sync는_인증_사용자의_컨텍스트를_한_번_동기화한다() {
        InterestService interestService = mock(InterestService.class);
        AgentContextSyncService contextSyncService = mock(AgentContextSyncService.class);
        InterestController controller = new InterestController(interestService, contextSyncService);

        ApiResponse<Void> response = controller.sync(new AuthPrincipal(42L, "user@bambi.test"));

        assertThat(response.isSuccess()).isTrue();
        verify(contextSyncService).syncUserContext(42L);
    }
}
