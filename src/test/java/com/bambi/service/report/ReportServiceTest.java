package com.bambi.service.report;

import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link ReportService}의 카드 공개 시각 연동 접근 제어를 검증한다. */
class ReportServiceTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final ReportService service = new ReportService(reportRepository, cardRepository);

    @Test
    void 소유자도_카드_공개_시각_전에는_본문을_볼_수_없다() {
        Report report = report(7L, 1L);
        when(reportRepository.findByPublicIdAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(report));
        when(cardRepository.existsReleasedByReportIdAndUserId(7L, 1L)).thenReturn(false);

        ApiException error = catchThrowableOfType(
                () -> service.get(1L, UUID.randomUUID().toString()), ApiException.class);

        assertThat(error).isNotNull();
    }

    @Test
    void 공개_시각이_지난_소유자_리포트는_본문을_반환한다() {
        Report report = report(7L, 1L);
        when(reportRepository.findByPublicIdAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(report));
        when(cardRepository.existsReleasedByReportIdAndUserId(7L, 1L)).thenReturn(true);

        assertThat(service.get(1L, UUID.randomUUID().toString())).isNotNull();
    }

    private Report report(Long id, Long userId) {
        Report report = mock(Report.class);
        when(report.getId()).thenReturn(id);
        when(report.getUserId()).thenReturn(userId);
        return report;
    }
}
