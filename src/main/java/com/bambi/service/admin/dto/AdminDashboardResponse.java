package com.bambi.service.admin.dto;

import java.util.List;

/**
 * 관리자 대시보드 개요 — 한 번의 조회로 운영 지표를 묶어 내린다.
 *
 * 모두 기존 데이터(users·reports·ai_*_logs) 집계라 새 테이블이 없다.
 * AI 지표는 요청 로그에 그 요청의 최신 응답을 붙여 파생한 상태({@link AdminAiLogResponse})를 센다.
 */
public record AdminDashboardResponse(
        Users users,
        Reports reports,
        AiCalls ai,
        Generations generations,                // 리포트 생성 수명주기(접수→카드)
        List<AdminAiLogResponse> recentFailures // 최근 실패한 AI 호출(최신순 일부)
) {

    /** 사용자 수 집계. total = active + inactive. */
    public record Users(long total, long active, long inactive, long joinedToday) {
    }

    /** 리포트 수 집계(soft delete 제외). */
    public record Reports(long total, long createdToday) {
    }

    /**
     * AI 호출 집계.
     *
     * <p>성공률의 분모는 <b>끝난 호출(success + failed)</b>이다. 아직 응답을 기다리는
     * PROCESSING 을 분모에 넣으면 호출이 몰릴 때마다 성공률이 떨어져 장애처럼 보인다 —
     * 운영자가 보려는 건 "판정 난 것 중 몇 %가 성공했나"라서 이쪽이 맞다.
     *
     * @param successRate  끝난 호출 중 성공 백분율(0~100 정수). 끝난 호출이 0건이면 0.
     * @param avgLatencyMs 성공 호출의 평균 소요시간(ms). 성공 건이 없으면 null.
     */
    public record AiCalls(
            long total,
            long success,
            long failed,
            long processing,
            int successRate,
            Integer avgLatencyMs) {
    }

    /**
     * 리포트 생성 수명주기 집계 — <b>접수(PENDING)부터 카드 발행(COMPLETED)까지</b>.
     *
     * <p>{@link AiCalls#avgLatencyMs} 와 재는 것이 다르다. 그쪽은 service→agent <b>HTTP 왕복</b>이고,
     * 생성 요청은 agent 가 202 로 즉시 응답하므로 수십 ms 로 나온다. 실제 리포트는 1~2분 걸린다.
     * 두 숫자를 같은 것으로 읽으면 "AI 가 61ms 만에 처리한다"는 오해가 생긴다.
     *
     * <p>이 집계가 필요한 이유: 2026-08-12 운영에서 리포트가 20분씩 걸렸는데 대시보드에는
     * 아무 이상이 안 보였다. AI 호출은 전부 성공(202)이었기 때문이다. 큐가 밀렸는지·좀비가
     * 쌓였는지는 이 수명주기를 봐야 알 수 있다.
     *
     * @param inProgress    아직 안 끝난 접수(PENDING·RUNNING·PUBLISHING). <b>기간 무관</b> —
     *                      어제 것이 물려 있으면 그것도 보여야 한다.
     * @param completedToday 오늘(KST) 접수분 중 완료된 건수
     * @param failedToday    오늘 접수분 중 실패·취소된 건수
     * @param avgSeconds     오늘 완료분의 평균 소요(초). 큐 대기를 포함한 사용자 체감 시간. 없으면 null
     * @param maxSeconds     오늘 완료분 중 최장 소요(초). 평균만 보면 꼬리가 안 보인다. 없으면 null
     */
    public record Generations(
            long inProgress,
            long completedToday,
            long failedToday,
            Integer avgSeconds,
            Integer maxSeconds) {
    }
}
