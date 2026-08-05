package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 사용자가 직접 "지금 생성"을 눌렀을 때, 스케줄러를 기다리지 않고 즉시 리포트 생성을 요청한다.
 * 스케줄러와 같은 {@link GenerationClient} 경계를 재사용하며 대상은 요청한 본인 1명이다.
 *
 * <p>멱등키는 분 단위라 같은 분 내 연타(더블클릭)는 Job 1개로 합쳐지고, 시간이 지나면 새로 생성된다
 * (일일 1회인 스케줄러와 달리 on-demand 는 반복 허용).
 */
@Service
public class OnDemandGenerationService {

    private static final Logger log = LoggerFactory.getLogger(OnDemandGenerationService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GenerationClient generationClient;
    private final String topic;
    private final String contentType;

    public OnDemandGenerationService(
            GenerationClient generationClient,
            @Value("${app.scheduler.generation.topic:오늘의 관심사 뉴스}") String topic,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        this.generationClient = generationClient;
        this.topic = topic;
        this.contentType = contentType;
    }

    /**
     * 요청한 사용자의 위키·관심사 기반으로 즉시 생성 Job 을 접수한다.
     * requestedTopic 을 주면(프론트가 위키 상위 관심사를 골라 넘기는 경로) 그 주제로, 비우면 기본값으로 생성한다.
     */
    public GenerationTriggerResponse generateForUser(long userId, String requestedTopic) {
        // TODO(송우 확정 대기): 백엔드가 직접 top 관심사를 뽑는 방식이면, 여기서 agent /interests 조회해 채운다.
        String effectiveTopic = hasText(requestedTopic) ? requestedTopic.strip() : topic;
        GenerationRequest request = new GenerationRequest(
                onDemandKey(userId), effectiveTopic, contentType, null, null);
        generationClient.requestGeneration(userId, request);
        log.info("[OnDemandGeneration] 즉시 생성 요청 userId={}, topic={}, idempotencyKey={}",
                userId, effectiveTopic, request.idempotencyKey());
        // TODO(송우 답): 펜딩 UI 용 job_id 필요하면 GenerationClient 반환형을 확장해 여기 담는다.
        return GenerationTriggerResponse.accepted(null);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** on-demand 멱등키(분 단위) — 연타는 1건, 시간 지나면 새 생성. */
    private String onDemandKey(long userId) {
        long minute = OffsetDateTime.now(KST).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
        return "ondemand-" + userId + "-" + contentType + "-" + minute;
    }
}
