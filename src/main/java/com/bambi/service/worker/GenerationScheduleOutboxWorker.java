package com.bambi.service.worker;

import com.bambi.service.agent.dto.AgentAcceptedJob;
import com.bambi.service.briefing.BriefingTopicService;
import com.bambi.service.generation.GenerationScheduler;
import com.bambi.service.generation.GenerationSubmissionService;
import com.bambi.service.generation.MorningBriefingGenerationService;
import com.bambi.service.generation.schedule.GenerationScheduleOutboxEvent;
import com.bambi.service.generation.schedule.GenerationScheduleOutboxService;
import com.bambi.service.generation.schedule.GenerationSchedulePhase;
import com.bambi.service.wiki.AgentWikiClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** 스케줄 Outbox를 짧게 점유하고 제한된 동시성으로 Agent 준비·생성 요청을 전달한다. */
@Component
@ConditionalOnProperty(
        name = "app.worker.generation-schedule.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class GenerationScheduleOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(GenerationScheduleOutboxWorker.class);

    private final GenerationScheduleOutboxService outbox;
    private final AgentWikiClient wikiClient;
    private final MorningBriefingGenerationService morningBriefingService;
    private final int batchLimit;
    private final int leaseSeconds;
    private final int maxAttempts;
    private final int retryBaseSeconds;
    private final int retryMaxSeconds;
    private final String contentType;
    private final String workerId;
    private final ExecutorService executor;
    private final AtomicBoolean polling = new AtomicBoolean(false);

    public GenerationScheduleOutboxWorker(
            GenerationScheduleOutboxService outbox,
            AgentWikiClient wikiClient,
            MorningBriefingGenerationService morningBriefingService,
            @Value("${app.worker.generation-schedule.batch-limit:20}") int batchLimit,
            @Value("${app.worker.generation-schedule.concurrency:4}") int concurrency,
            @Value("${app.worker.generation-schedule.lease-seconds:120}") int leaseSeconds,
            @Value("${app.worker.generation-schedule.max-attempts:8}") int maxAttempts,
            @Value("${app.worker.generation-schedule.retry-base-seconds:5}") int retryBaseSeconds,
            @Value("${app.worker.generation-schedule.retry-max-seconds:300}") int retryMaxSeconds,
            @Value("${app.worker.generation-schedule.worker-id:service-worker-01}") String workerId,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        validateSettings(batchLimit, concurrency, leaseSeconds, maxAttempts,
                retryBaseSeconds, retryMaxSeconds);
        this.outbox = outbox;
        this.wikiClient = wikiClient;
        this.morningBriefingService = morningBriefingService;
        this.batchLimit = batchLimit;
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
        this.retryBaseSeconds = retryBaseSeconds;
        this.retryMaxSeconds = retryMaxSeconds;
        this.contentType = contentType;
        this.workerId = workerId + "-generation-" + UUID.randomUUID().toString().substring(0, 8);
        this.executor = Executors.newFixedThreadPool(
                concurrency,
                Thread.ofPlatform().name("generation-outbox-", 0).factory());
    }

    /** 한 Batch를 점유해 제한된 수만 동시에 처리하고 모든 결과가 종결될 때까지 기다린다. */
    @Scheduled(
            fixedDelayString = "${app.worker.generation-schedule.poll-interval-ms:3000}",
            initialDelayString = "${app.worker.generation-schedule.initial-delay-ms:5000}")
    public void poll() {
        if (!polling.compareAndSet(false, true)) {
            return;
        }
        try {
            List<GenerationScheduleOutboxEvent> events = outbox.claim(
                    batchLimit, workerId, leaseSeconds);
            CompletableFuture<?>[] tasks = events.stream()
                    .map(event -> CompletableFuture.runAsync(() -> process(event), executor))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(tasks).join();
        } finally {
            polling.set(false);
        }
    }

    private void process(GenerationScheduleOutboxEvent event) {
        try {
            String agentJobId = relay(event);
            if (!outbox.markDelivered(event.getId(), workerId, agentJobId)) {
                log.warn("[GenerationScheduleOutbox] 완료 소유권 상실 eventId={}", event.getId());
            }
        } catch (Exception error) {
            log.warn("[GenerationScheduleOutbox] Agent 전달 실패 — 재시도 eventId={}, phase={}",
                    event.getId(), event.getPhase(), error);
            if (!outbox.markRetry(event.getId(), workerId, error,
                    maxAttempts, retryBaseSeconds, retryMaxSeconds)) {
                log.warn("[GenerationScheduleOutbox] 재시도 소유권 상실 eventId={}", event.getId());
            }
        }
    }

    private String relay(GenerationScheduleOutboxEvent event) {
        if (event.getPhase() == GenerationSchedulePhase.BRIEFING_PREPARATION) {
            AgentAcceptedJob accepted = wikiClient.prepareBriefing(
                    event.getUserId(),
                    event.getScheduleDate(),
                    briefingPreparationIdempotencyKey(event),
                    BriefingTopicService.MAX_TOPICS);
            if (accepted == null || accepted.jobId() == null || accepted.jobId().isBlank()) {
                throw new IllegalStateException("Agent 준비 응답에 job_id가 없다.");
            }
            return accepted.jobId();
        }
        return morningBriefingService.submit(
                        event.getUserId(),
                        GenerationScheduler.idempotencyKey(
                                event.getScheduleDate(), event.getUserId(), contentType),
                        event.getScheduleDate())
                .map(GenerationSubmissionService.Submission::agentJobId)
                .orElse(null);
    }

    /** 같은 사용자·날짜의 준비 Job을 하나로 합치는 Agent 멱등키를 만든다. */
    static String briefingPreparationIdempotencyKey(GenerationScheduleOutboxEvent event) {
        return event.getScheduleDate() + "-" + event.getUserId() + "-briefing_preparation";
    }

    private void validateSettings(
            int batchLimit,
            int concurrency,
            int leaseSeconds,
            int maxAttempts,
            int retryBaseSeconds,
            int retryMaxSeconds) {
        if (batchLimit < 1 || concurrency < 1 || leaseSeconds < 1 || maxAttempts < 1
                || retryBaseSeconds < 1 || retryMaxSeconds < retryBaseSeconds) {
            throw new IllegalArgumentException("generation-schedule Worker 설정값이 유효하지 않다.");
        }
    }

    /** 애플리케이션 종료 시 새 작업 접수를 막고 Worker 스레드를 정리한다. */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
