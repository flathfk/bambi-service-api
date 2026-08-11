package com.bambi.service.worker;

import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.generation.GenerationSubmissionService;
import com.bambi.service.generation.MorningBriefingGenerationService;
import com.bambi.service.generation.schedule.GenerationScheduleOutboxEvent;
import com.bambi.service.generation.schedule.GenerationScheduleOutboxService;
import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.wiki.AgentWikiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link GenerationScheduleOutboxWorker}의 단계별 전달·재시도·제한 동시성을 검증한다. */
class GenerationScheduleOutboxWorkerTest {

    private final GenerationScheduleOutboxService outbox =
            mock(GenerationScheduleOutboxService.class);
    private final AgentWikiClient wikiClient = mock(AgentWikiClient.class);
    private final MorningBriefingGenerationService morningService =
            mock(MorningBriefingGenerationService.class);
    private final GenerationScheduleOutboxWorker worker = new GenerationScheduleOutboxWorker(
            outbox, wikiClient, morningService,
            20, 2, 120, 8, 5, 300, "worker-test", "interest_news_card");

    @AfterEach
    void shutDownWorker() {
        worker.shutdown();
    }

    @Test
    void 준비_이벤트는_REPORT_022_Job으로_접수하고_완료한다() {
        GenerationScheduleOutboxEvent event = event(
                1L, 7L, GenerationSchedulePhase.BRIEFING_PREPARATION);
        when(outbox.claim(eq(20), anyString(), eq(120))).thenReturn(List.of(event));
        when(wikiClient.prepareBriefing(
                7L, LocalDate.of(2026, 8, 12),
                "2026-08-12-7-briefing_preparation", 3))
                .thenReturn(new AgentAcceptedJob("prep-job-7", "PENDING"));
        when(outbox.markDelivered(eq(1L), anyString(), eq("prep-job-7"))).thenReturn(true);

        worker.poll();

        verify(wikiClient).prepareBriefing(
                7L, LocalDate.of(2026, 8, 12),
                "2026-08-12-7-briefing_preparation", 3);
        verify(outbox).markDelivered(eq(1L), anyString(), eq("prep-job-7"));
    }

    @Test
    void 생성_이벤트는_같은_날짜의_주제와_생성_요청을_사용한다() {
        GenerationScheduleOutboxEvent event = event(
                2L, 9L, GenerationSchedulePhase.MORNING_GENERATION);
        when(outbox.claim(eq(20), anyString(), eq(120))).thenReturn(List.of(event));
        when(morningService.submit(
                9L, "2026-08-12-9-interest_news_card", LocalDate.of(2026, 8, 12)))
                .thenReturn(Optional.of(
                        new GenerationSubmissionService.Submission("pending-9", "generation-job-9")));
        when(outbox.markDelivered(eq(2L), anyString(), eq("generation-job-9"))).thenReturn(true);

        worker.poll();

        verify(morningService).submit(
                9L, "2026-08-12-9-interest_news_card", LocalDate.of(2026, 8, 12));
        verify(outbox).markDelivered(eq(2L), anyString(), eq("generation-job-9"));
    }

    @Test
    void 생성할_주제가_없으면_Agent_Job없이_정상_종결한다() {
        GenerationScheduleOutboxEvent event = event(
                3L, 11L, GenerationSchedulePhase.MORNING_GENERATION);
        when(outbox.claim(eq(20), anyString(), eq(120))).thenReturn(List.of(event));
        when(morningService.submit(anyLong(), anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(outbox.markDelivered(eq(3L), anyString(), eq(null))).thenReturn(true);

        worker.poll();

        verify(outbox).markDelivered(eq(3L), anyString(), eq(null));
    }

    @Test
    void Agent_실패는_설정한_재시도_정책으로_되돌린다() {
        GenerationScheduleOutboxEvent event = event(
                4L, 7L, GenerationSchedulePhase.BRIEFING_PREPARATION);
        RuntimeException failure = new RuntimeException("agent down");
        when(outbox.claim(eq(20), anyString(), eq(120))).thenReturn(List.of(event));
        when(wikiClient.prepareBriefing(anyLong(), any(), anyString(), anyInt()))
                .thenThrow(failure);
        when(outbox.markRetry(eq(4L), anyString(), eq(failure), eq(8), eq(5), eq(300)))
                .thenReturn(true);

        worker.poll();

        verify(outbox).markRetry(eq(4L), anyString(), eq(failure), eq(8), eq(5), eq(300));
    }

    @Test
    void 한_Batch의_동시_Agent_호출은_설정값을_넘지_않는다() throws Exception {
        List<GenerationScheduleOutboxEvent> events = List.of(
                event(10L, 10L, GenerationSchedulePhase.BRIEFING_PREPARATION),
                event(11L, 11L, GenerationSchedulePhase.BRIEFING_PREPARATION),
                event(12L, 12L, GenerationSchedulePhase.BRIEFING_PREPARATION),
                event(13L, 13L, GenerationSchedulePhase.BRIEFING_PREPARATION));
        when(outbox.claim(eq(20), anyString(), eq(120))).thenReturn(events);
        when(outbox.markDelivered(anyLong(), anyString(), anyString())).thenReturn(true);

        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch firstWave = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(wikiClient.prepareBriefing(anyLong(), any(), anyString(), anyInt()))
                .thenAnswer(invocation -> {
                    int current = active.incrementAndGet();
                    maximum.accumulateAndGet(current, Math::max);
                    firstWave.countDown();
                    try {
                        release.await(3, TimeUnit.SECONDS);
                    } finally {
                        active.decrementAndGet();
                    }
                    return new AgentAcceptedJob("prep-job", "PENDING");
                });

        CompletableFuture<Void> polling = CompletableFuture.runAsync(worker::poll);
        assertThat(firstWave.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(maximum.get()).isEqualTo(2);
        release.countDown();
        polling.get(5, TimeUnit.SECONDS);

        verify(wikiClient, times(4)).prepareBriefing(anyLong(), any(), anyString(), anyInt());
        assertThat(maximum.get()).isLessThanOrEqualTo(2);
    }

    private GenerationScheduleOutboxEvent event(
            long id,
            long userId,
            GenerationSchedulePhase phase) {
        GenerationScheduleOutboxEvent event = mock(GenerationScheduleOutboxEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getUserId()).thenReturn(userId);
        when(event.getPhase()).thenReturn(phase);
        when(event.getScheduleDate()).thenReturn(LocalDate.of(2026, 8, 12));
        return event;
    }
}
