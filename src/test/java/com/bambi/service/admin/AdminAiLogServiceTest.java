package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogDetailResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AdminAiLogService#getLogDetail} 검증 — 상세 조회의 없음/있음 분기.
 */
class AdminAiLogServiceTest {

    private final AiRequestLogRepository requestRepo = mock(AiRequestLogRepository.class);
    private final AiResponseLogRepository responseRepo = mock(AiResponseLogRepository.class);
    private final AdminAiLogService service = new AdminAiLogService(requestRepo, responseRepo);

    @Test
    void 요청_로그가_없으면_NOT_FOUND() {
        when(requestRepo.findById(anyLong())).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> service.getLogDetail(999L), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 요청은_있고_응답이_아직_없으면_처리중_상세를_돌려준다() {
        AiRequestLog request = new AiRequestLog(null, "/internal/v1/users/1/context", "{}");
        when(requestRepo.findById(7L)).thenReturn(Optional.of(request));
        when(responseRepo.findFirstByRequestIdOrderByCreatedAtDesc(7L)).thenReturn(Optional.empty());

        AdminAiLogDetailResponse detail = service.getLogDetail(7L);

        assertThat(detail.status()).isEqualTo("PROCESSING");
        assertThat(detail.responseBody()).isNull();
        assertThat(detail.endpoint()).isEqualTo("/internal/v1/users/1/context");
    }
}
