package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogResponse;
import com.bambi.service.admin.dto.AdminDashboardResponse;
import com.bambi.service.generation.GenerationPendingRepository;
import com.bambi.service.report.ReportRepository;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AdminDashboardService} — 집계 규칙 검증.
 *
 * 수 세기 자체는 DB(count 쿼리)가 하므로 여기선 서비스가 정하는 값,
 * 즉 성공률의 분모·평균 응답시간·최근 실패 추림을 확인한다.
 */
class AdminDashboardServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final AdminAiLogService aiLogService = mock(AdminAiLogService.class);
    private final GenerationPendingRepository generationPendingRepository =
            mock(GenerationPendingRepository.class);
    private final AdminDashboardService service = new AdminDashboardService(
            userRepository, reportRepository, aiLogService, generationPendingRepository);

    private AdminAiLogResponse log(long id, String status, Integer latencyMs) {
        return new AdminAiLogResponse(
                id, OffsetDateTime.now(), "a@bambi.test", "/internal/v1/users/1/context",
                status, status.equals("SUCCESS") ? 200 : null, latencyMs);
    }

    private void givenLogs(AdminAiLogResponse... logs) {
        when(aiLogService.listLogs()).thenReturn(List.of(logs));
    }

    @Test
    @DisplayName("성공률의 분모는 끝난 호출 — 처리 중은 빼고 센다")
    void successRateExcludesProcessing() {
        givenLogs(
                log(1, "SUCCESS", 100),
                log(2, "SUCCESS", 300),
                log(3, "FAILED", null),
                log(4, "PROCESSING", null));

        AdminDashboardResponse.AiCalls ai = service.getOverview().ai();

        assertThat(ai.total()).isEqualTo(4);
        assertThat(ai.success()).isEqualTo(2);
        assertThat(ai.failed()).isEqualTo(1);
        assertThat(ai.processing()).isEqualTo(1);
        // 처리 중을 분모에 넣었다면 50%가 됐을 것 — 끝난 3건 중 2건이라 67%.
        assertThat(ai.successRate()).isEqualTo(67);
    }

    @Test
    @DisplayName("호출이 하나도 없으면 성공률 0, 평균 응답시간 null")
    void emptyLogsGiveZeroRateAndNullLatency() {
        givenLogs();

        AdminDashboardResponse.AiCalls ai = service.getOverview().ai();

        assertThat(ai.total()).isZero();
        assertThat(ai.successRate()).isZero();
        assertThat(ai.avgLatencyMs()).isNull();
    }

    @Test
    @DisplayName("평균 응답시간은 성공 호출만 — 실패·처리중의 null 은 섞이지 않는다")
    void averageLatencyCountsSuccessOnly() {
        givenLogs(
                log(1, "SUCCESS", 100),
                log(2, "SUCCESS", 200),
                log(3, "FAILED", 9_999),   // 실패는 오래 걸려도 평균에 안 들어간다
                log(4, "PROCESSING", null));

        assertThat(service.getOverview().ai().avgLatencyMs()).isEqualTo(150);
    }

    @Test
    @DisplayName("최근 실패는 실패만 최신순 5건까지")
    void recentFailuresAreCappedAndFailedOnly() {
        givenLogs(
                log(1, "FAILED", null), log(2, "SUCCESS", 10), log(3, "FAILED", null),
                log(4, "FAILED", null), log(5, "FAILED", null), log(6, "FAILED", null),
                log(7, "FAILED", null));

        List<AdminAiLogResponse> failures = service.getOverview().recentFailures();

        assertThat(failures).hasSize(5);
        assertThat(failures).allMatch(failure -> "FAILED".equals(failure.status()));
        assertThat(failures.get(0).id()).isEqualTo(1L); // 목록이 최신순이라 앞에서 자른다
    }

    @Test
    @DisplayName("사용자 total 은 비활성까지 포함한 누적 — 관리자 목록 길이와 맞춘다")
    void userTotalIncludesInactive() {
        givenLogs();
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByDeletedAtIsNull()).thenReturn(7L);
        when(userRepository.countByDeletedAtIsNotNull()).thenReturn(3L);
        when(userRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(2L);
        when(reportRepository.countByDeletedAtIsNull()).thenReturn(42L);
        when(reportRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqual(any()))
                .thenReturn(5L);

        AdminDashboardResponse overview = service.getOverview();

        assertThat(overview.users().total()).isEqualTo(10);
        assertThat(overview.users().active()).isEqualTo(7);
        assertThat(overview.users().inactive()).isEqualTo(3);
        assertThat(overview.users().joinedToday()).isEqualTo(2);
        assertThat(overview.reports().total()).isEqualTo(42);
        assertThat(overview.reports().createdToday()).isEqualTo(5);
    }

    /* ===== 리포트 생성 수명주기 (2026-08-12) ===== */

    @Test
    @DisplayName("생성 현황은 접수 테이블에서 센다 — 진행 중은 기간을 안 자른다")
    void generationsCountFromPendings() {
        givenLogs();
        when(generationPendingRepository.countByStatusIn(
                List.of("PENDING", "RUNNING", "PUBLISHING"))).thenReturn(8L);
        when(generationPendingRepository.countByStatusInAndCreatedAtGreaterThanEqual(
                eq(List.of("COMPLETED")), any())).thenReturn(103L);
        when(generationPendingRepository.countByStatusInAndCreatedAtGreaterThanEqual(
                eq(List.of("FAILED", "CANCELLED")), any())).thenReturn(4L);
        when(generationPendingRepository.averageCompletionSeconds(any())).thenReturn(94.0);
        when(generationPendingRepository.maxCompletionSeconds(any())).thenReturn(764.0);

        AdminDashboardResponse.Generations generations = service.getOverview().generations();

        assertThat(generations.inProgress()).isEqualTo(8);
        assertThat(generations.completedToday()).isEqualTo(103);
        assertThat(generations.failedToday()).isEqualTo(4);
        assertThat(generations.avgSeconds()).isEqualTo(94);
        assertThat(generations.maxSeconds()).isEqualTo(764);
    }

    @Test
    @DisplayName("완료 건이 없으면 소요시간은 null — 0 으로 내리면 '0초에 끝났다'로 읽힌다")
    void noCompletionsGiveNullSeconds() {
        givenLogs();
        when(generationPendingRepository.averageCompletionSeconds(any())).thenReturn(null);
        when(generationPendingRepository.maxCompletionSeconds(any())).thenReturn(null);

        AdminDashboardResponse.Generations generations = service.getOverview().generations();

        assertThat(generations.avgSeconds()).isNull();
        assertThat(generations.maxSeconds()).isNull();
    }

    @Test
    @DisplayName("AI 호출 지표와 생성 소요시간은 서로 다른 것을 잰다")
    void aiLatencyAndGenerationSecondsAreDifferentMetrics() {
        // agent 는 생성 요청에 202 로 즉시 응답한다 — HTTP 왕복은 61ms 인데
        // 실제 리포트는 94초 걸린다. 둘을 같은 칸에 놓으면 "AI 가 61ms" 로 읽힌다.
        givenLogs(log(1, "SUCCESS", 61));
        when(generationPendingRepository.averageCompletionSeconds(any())).thenReturn(94.0);

        AdminDashboardResponse overview = service.getOverview();

        assertThat(overview.ai().avgLatencyMs()).isEqualTo(61);        // HTTP 접수 왕복
        assertThat(overview.generations().avgSeconds()).isEqualTo(94); // 접수→카드
    }
}
