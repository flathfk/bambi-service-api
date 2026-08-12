package com.bambi.service.generation;

import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.generation.schedule.GenerationSchedulePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/** 05시 아침 브리핑 사전 생성 작업을 DB Outbox에 발행하는 경량 스케줄러. */
@Component
@ConditionalOnProperty(name = "app.scheduler.generation.enabled", havingValue = "true")
public class GenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(GenerationScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GenerationSchedulePublisher publisher;

    public GenerationScheduler(GenerationSchedulePublisher publisher) {
        this.publisher = publisher;
    }

    /** 매일 지정 시각에 오늘 활성 사용자의 생성 작업을 원자적으로 적재한다. */
    @Scheduled(cron = "${app.scheduler.generation.cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void triggerDailyGeneration() {
        LocalDate scheduleDate = LocalDate.now(KST);
        GenerationSchedulePublisher.PublicationResult result = publisher.publish(
                GenerationSchedulePhase.MORNING_GENERATION, scheduleDate);
        log.info("[GenerationScheduler] Outbox 발행 date={}, status={}, users={}",
                scheduleDate, result.status(), result.userCount());
    }

    /** 날짜·사용자·콘텐츠 유형으로 Agent 생성 요청 멱등키를 만든다. */
    public static String idempotencyKey(LocalDate window, long userId, String contentType) {
        return window + "-" + userId + "-" + contentType;
    }
}
