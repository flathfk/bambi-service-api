package com.bambi.service.briefing;

import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.generation.schedule.GenerationSchedulePublisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link BriefingTopicPrefetchScheduler}가 03시 외부 호출 대신 준비 Outbox만 발행하는지 검증한다. */
class BriefingTopicPrefetchSchedulerTest {

    private final GenerationSchedulePublisher publisher = mock(GenerationSchedulePublisher.class);
    private final BriefingTopicPrefetchScheduler scheduler = new BriefingTopicPrefetchScheduler(publisher);

    @Test
    void 브리핑_준비_단계의_오늘_Outbox만_발행한다() {
        when(publisher.publish(eq(GenerationSchedulePhase.BRIEFING_PREPARATION), any(LocalDate.class)))
                .thenReturn(new GenerationSchedulePublisher.PublicationResult(
                        GenerationSchedulePublisher.PublicationStatus.PUBLISHED, 3));

        scheduler.prefetchBriefingTopics();

        verify(publisher).publish(
                eq(GenerationSchedulePhase.BRIEFING_PREPARATION), any(LocalDate.class));
    }
}
