package com.bambi.service.briefing;

import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.generation.schedule.GenerationSchedulePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/** 03시 아침 브리핑 준비 작업을 DB Outbox에 발행하는 경량 스케줄러. */
@Component
@ConditionalOnProperty(name = "app.scheduler.briefing-prefetch.enabled", havingValue = "true")
public class BriefingTopicPrefetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BriefingTopicPrefetchScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GenerationSchedulePublisher publisher;

    public BriefingTopicPrefetchScheduler(GenerationSchedulePublisher publisher) {
        this.publisher = publisher;
    }

    /** 매일 지정 시각에 오늘 활성 사용자의 브리핑 준비 작업을 원자적으로 적재한다. */
    @Scheduled(cron = "${app.scheduler.briefing-prefetch.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void prefetchBriefingTopics() {
        LocalDate scheduleDate = LocalDate.now(KST);
        GenerationSchedulePublisher.PublicationResult result = publisher.publish(
                GenerationSchedulePhase.BRIEFING_PREPARATION, scheduleDate);
        log.info("[BriefingPrefetch] Outbox 발행 date={}, status={}, users={}",
                scheduleDate, result.status(), result.userCount());
    }
}
