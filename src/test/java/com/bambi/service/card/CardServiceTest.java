package com.bambi.service.card;

import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CardService#changeVisibility} 검증 — 소유자 공개설정 변경 / 없는 카드 404 / 잘못된 값 도메인 방어.
 */
class CardServiceTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final com.bambi.service.report.ReportRepository reportRepository =
            mock(com.bambi.service.report.ReportRepository.class);
    private final CardService service = new CardService(cardRepository, reportRepository);

    @Test
    void 소유자는_자기_카드를_공개로_바꾼다() {
        Card card = new Card(1L, "제목", "요약", "왜 당신에게");   // 기본 PRIVATE
        when(cardRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(any(), eq(1L)))
                .thenReturn(Optional.of(card));

        CardResponse res = service.changeVisibility(1L, UUID.randomUUID().toString(), "PUBLIC");

        assertThat(card.getVisibility()).isEqualTo("PUBLIC");
        assertThat(res.publicId()).isEqualTo(card.getPublicId());
    }

    @Test
    void 남의_카드는_존재_노출_없이_NOT_FOUND() {
        when(cardRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> service.changeVisibility(1L, UUID.randomUUID().toString(), "PUBLIC"),
                ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 카드_도메인은_허용되지_않은_공개값을_거부한다() {
        Card card = new Card(1L, "제목", "요약", "왜 당신에게");

        assertThatThrownBy(() -> card.changeVisibility("BOGUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
