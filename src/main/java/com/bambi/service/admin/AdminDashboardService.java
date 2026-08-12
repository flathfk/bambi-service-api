package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogResponse;
import com.bambi.service.admin.dto.AdminDashboardResponse;
import com.bambi.service.generation.GenerationPendingRepository;
import com.bambi.service.report.ReportRepository;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * 관리자 대시보드 — 첫 화면에 띄울 운영 지표를 한 번에 모은다.
 *
 * <p>새 테이블 없이 기존 데이터(users·reports·ai_*_logs)를 세기만 한다. 사용자·리포트는
 * count 쿼리로 DB 에서 세고, AI 지표는 {@link AdminAiLogService#listLogs()} 가 만든 목록을
 * 그대로 집계한다. 상태(SUCCESS/FAILED/PROCESSING)가 컬럼이 아니라 요청+최신응답에서
 * 파생되는 값이라, 같은 규칙을 두 번 구현해 대시보드 숫자와 로그 화면이 어긋나는 걸 막는다.
 * 그 대신 로그 전체를 한 번 훑는 비용을 진다 — 로그 화면과 같은 비용이고 적재량이 아직
 * 작아 지금은 문제되지 않는다. 커지면 집계 쿼리로 내리면 되고, 그때 벤치로 확인한다.
 *
 * <p>"오늘"의 기준은 KST 자정이다. 컨테이너 시간대(UTC)로 자르면 한국 새벽 시간대의
 * 가입·생성이 전날로 밀려 운영자가 보는 숫자와 어긋난다.
 */
@Service
public class AdminDashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 대시보드에 미리보기로 띄울 최근 실패 건수. 자세히는 로그 화면(?status=FAILED)에서 본다. */
    private static final int RECENT_FAILURE_LIMIT = 5;

    /** 아직 안 끝난 접수로 볼 상태들. 완료·실패·취소만 종료로 친다. */
    private static final List<String> IN_PROGRESS_STATUSES = List.of("PENDING", "RUNNING", "PUBLISHING");

    /** 실패로 볼 상태들. 사용자 취소도 "카드가 안 나온 접수"라 같이 센다. */
    private static final List<String> FAILED_STATUSES = List.of("FAILED", "CANCELLED");

    /**
     * 이 시간을 넘겨 미종료면 "진행 중"이 아니라 <b>정체</b>로 본다.
     *
     * <p>2시간인 이유: agent 리스가 10분이고 최대 3회 재시도라 순수 처리만 30분이 상한이다.
     * 여기에 아침 브리핑이 07:00 에 한꺼번에 몰릴 때의 큐 대기(실측 40~45분)를 더해도
     * 정상 건은 2시간 안에 끝난다. 실제로 문제였던 고아 건들은 2~5<b>일</b>이라 여유롭게 갈린다.
     */
    private static final Duration STALLED_AFTER = Duration.ofHours(2);

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final AdminAiLogService adminAiLogService;
    private final GenerationPendingRepository generationPendingRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 ReportRepository reportRepository,
                                 AdminAiLogService adminAiLogService,
                                 GenerationPendingRepository generationPendingRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.adminAiLogService = adminAiLogService;
        this.generationPendingRepository = generationPendingRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getOverview() {
        OffsetDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST).toOffsetDateTime();
        List<AdminAiLogResponse> logs = adminAiLogService.listLogs();

        return new AdminDashboardResponse(
                countUsers(todayStart),
                countReports(todayStart),
                summarizeAiCalls(logs),
                summarizeGenerations(todayStart),
                recentFailures(logs));
    }

    /**
     * 리포트 생성 수명주기(접수→카드) 집계.
     *
     * <p>AI 호출 지표(ai_*_logs)로는 이걸 못 본다 — 생성 요청은 agent 가 202 로 즉시 응답해
     * "호출 성공"으로 끝나기 때문이다. 실제로 리포트가 몇 분 걸렸는지, 큐에 몇 건이 물려 있는지는
     * 접수 테이블의 수명주기를 봐야 한다(2026-08-12 운영: 리포트 20분인데 대시보드는 정상 표시).
     */
    private AdminDashboardResponse.Generations summarizeGenerations(OffsetDateTime todayStart) {
        OffsetDateTime stalledBefore = OffsetDateTime.now().minus(STALLED_AFTER);
        return new AdminDashboardResponse.Generations(
                generationPendingRepository.countByStatusInAndCreatedAtGreaterThanEqual(
                        IN_PROGRESS_STATUSES, stalledBefore),
                generationPendingRepository.countByStatusInAndCreatedAtLessThan(
                        IN_PROGRESS_STATUSES, stalledBefore),
                generationPendingRepository.countByStatusInAndCreatedAtGreaterThanEqual(
                        List.of("COMPLETED"), todayStart),
                generationPendingRepository.countByStatusInAndCreatedAtGreaterThanEqual(
                        FAILED_STATUSES, todayStart),
                toSeconds(generationPendingRepository.medianCompletionSeconds(todayStart)),
                toSeconds(generationPendingRepository.maxCompletionSeconds(todayStart)));
    }

    /** 완료 건이 없으면 집계가 null 이다 — 0 으로 바꾸지 않는다(0초로 오해된다). */
    private Integer toSeconds(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    /**
     * 사용자 수. total 은 비활성(soft delete)까지 포함한 누적이다 — 관리자 목록이
     * 탈퇴 계정도 INACTIVE 로 함께 보여주므로 목록 길이와 total 이 맞아야 한다.
     */
    private AdminDashboardResponse.Users countUsers(OffsetDateTime todayStart) {
        return new AdminDashboardResponse.Users(
                userRepository.count(),
                userRepository.countByDeletedAtIsNull(),
                userRepository.countByDeletedAtIsNotNull(),
                userRepository.countByCreatedAtGreaterThanEqual(todayStart));
    }

    private AdminDashboardResponse.Reports countReports(OffsetDateTime todayStart) {
        return new AdminDashboardResponse.Reports(
                reportRepository.countByDeletedAtIsNull(),
                reportRepository.countByDeletedAtIsNullAndCreatedAtGreaterThanEqual(todayStart));
    }

    /** 상태별 건수와 성공률·평균 응답시간. 평균은 소요시간이 기록된 성공 호출만 대상으로 한다. */
    private AdminDashboardResponse.AiCalls summarizeAiCalls(List<AdminAiLogResponse> logs) {
        long success = countByStatus(logs, AdminAiLogService.STATUS_SUCCESS);
        long failed = countByStatus(logs, AdminAiLogService.STATUS_FAILED);
        long processing = countByStatus(logs, AdminAiLogService.STATUS_PROCESSING);

        long settled = success + failed;
        int successRate = settled == 0 ? 0 : (int) Math.round(success * 100.0 / settled);

        return new AdminDashboardResponse.AiCalls(
                logs.size(),
                success,
                failed,
                processing,
                successRate,
                averageSuccessLatencyMs(logs));
    }

    private long countByStatus(List<AdminAiLogResponse> logs, String status) {
        return logs.stream().filter(log -> status.equals(log.status())).count();
    }

    /** 성공 호출의 평균 소요시간(ms). 성공이 없거나 소요시간이 안 남았으면 null(= 표시 안 함). */
    private Integer averageSuccessLatencyMs(List<AdminAiLogResponse> logs) {
        OptionalDouble average = logs.stream()
                .filter(log -> AdminAiLogService.STATUS_SUCCESS.equals(log.status()))
                .map(AdminAiLogResponse::latencyMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        return average.isPresent() ? (int) Math.round(average.getAsDouble()) : null;
    }

    /** 최근 실패 미리보기. 목록이 이미 최신순이라 앞에서 자르면 된다. */
    private List<AdminAiLogResponse> recentFailures(List<AdminAiLogResponse> logs) {
        return logs.stream()
                .filter(log -> AdminAiLogService.STATUS_FAILED.equals(log.status()))
                .limit(RECENT_FAILURE_LIMIT)
                .toList();
    }
}
