package com.bambi.service.generation;

import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.generation.schedule.GenerationSchedulePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link GenerationScheduler}가 05시 외부 호출 대신 생성 Outbox만 발행하는지 검증한다. */
class GenerationSchedulerTest {

    private final GenerationSchedulePublisher publisher = mock(GenerationSchedulePublisher.class);
    private final GenerationScheduler scheduler = new GenerationScheduler(publisher);

    @Test
    void 멱등키는_날짜윈도우_userId_contentType_규약이다() {
        String key = GenerationScheduler.idempotencyKey(
                LocalDate.of(2026, 7, 30), 23L, "interest_news_card");

        assertThat(key).isEqualTo("2026-07-30-23-interest_news_card");
    }

    @Test
    void 생성_단계의_오늘_Outbox만_발행한다() {
        when(publisher.publish(eq(GenerationSchedulePhase.MORNING_GENERATION), any(LocalDate.class)))
                .thenReturn(new GenerationSchedulePublisher.PublicationResult(
                        GenerationSchedulePublisher.PublicationStatus.PUBLISHED, 3));

        scheduler.triggerDailyGeneration();

        ArgumentCaptor<LocalDate> date = ArgumentCaptor.forClass(LocalDate.class);
        verify(publisher).publish(eq(GenerationSchedulePhase.MORNING_GENERATION), date.capture());
        assertThat(date.getValue()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
    }

    @Test
    void 기본_생성_시각은_05시다() throws NoSuchMethodException {
        Scheduled scheduled = GenerationScheduler.class
                .getMethod("triggerDailyGeneration")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${app.scheduler.generation.cron:0 0 5 * * *}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }
}
