package com.bambi.service.generation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 정기 아침 브리핑만 생성 시각과 사용자 노출 시각을 분리한다. */
@Component
public class MorningBriefingReleasePolicy {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Pattern SCHEDULED_KEY = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})-\\d+-.+$");

    private final int releaseHour;

    public MorningBriefingReleasePolicy(
            @Value("${app.scheduler.generation.release-hour:7}") int releaseHour) {
        if (releaseHour < 0 || releaseHour > 23) {
            throw new IllegalArgumentException("아침 브리핑 공개 시각은 0~23시여야 한다.");
        }
        this.releaseHour = releaseHour;
    }

    /** 현재 시각을 기준으로 요청 결과의 사용자 노출 시각을 계산한다. */
    public OffsetDateTime availableAt(String reportType, String idempotencyKey) {
        return availableAt(reportType, idempotencyKey, OffsetDateTime.now(KST));
    }

    /** 테스트 가능한 기준 시각으로 요청 결과의 사용자 노출 시각을 계산한다. */
    OffsetDateTime availableAt(
            String reportType,
            String idempotencyKey,
            OffsetDateTime now) {
        if (!GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING.equals(reportType)
                || idempotencyKey == null) {
            return now;
        }
        Matcher matcher = SCHEDULED_KEY.matcher(idempotencyKey);
        if (!matcher.matches()) {
            // 개발용 즉시 생성(dev-morning-...) 등은 정기 브리핑과 같은 report_type을
            // 사용해도 기다리지 않는다.
            return now;
        }
        LocalDate scheduleDate = LocalDate.parse(matcher.group(1));
        return scheduleDate.atTime(releaseHour, 0).atZone(KST).toOffsetDateTime();
    }
}
