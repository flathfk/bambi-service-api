package com.bambi.service.generation;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import com.bambi.service.wiki.AgentWikiClient;
import com.bambi.service.wiki.dto.WikiTagsResponse;
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
 * <p>온디맨드는 관심 자료 한 건이 아니라 <b>사용자 관심사 전체를 종합</b>하는 보고서다(제품 정의, 여진 확인).
 * 그래서 프론트가 topic 을 고르지 않고, 서버가 agent 관심사(위키 태그)를 확인해 생성한다.
 * 관심사가 하나도 없으면 종합할 게 없으므로 {@link ErrorCode#VALIDATION_ERROR} 로 거절한다
 * (프론트 constants/errors.ts 가 알려진 코드만 매핑해서 표준 코드로 통일).
 *
 * <p>멱등키는 분 단위라 같은 분 내 연타(더블클릭)는 Job 1개로 합쳐진다. "이미 진행 중인 작업" 완전 차단
 * (GENERATION_IN_PROGRESS)은 생성 작업 상태 영속화가 필요해 후속 과제다(여진 5번 — 펜딩 목록 API와 함께).
 */
@Service
public class OnDemandGenerationService {

    private static final Logger log = LoggerFactory.getLogger(OnDemandGenerationService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GenerationClient generationClient;
    private final AgentWikiClient wikiClient;
    private final String topic;
    private final String contentType;

    public OnDemandGenerationService(
            GenerationClient generationClient,
            AgentWikiClient wikiClient,
            @Value("${app.generation.on-demand.topic:내 관심사 종합 브리핑}") String topic,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        this.generationClient = generationClient;
        this.wikiClient = wikiClient;
        this.topic = topic;
        this.contentType = contentType;
    }

    /**
     * 요청한 사용자의 관심사 전체를 종합해 즉시 생성 Job 을 접수하고 job_id 를 반환한다.
     * 관심사가 없으면 VALIDATION_ERROR. 실제 종합은 agent 가 사용자 위키 컨텍스트로 수행한다(topic 은 표시용 라벨).
     */
    public GenerationTriggerResponse generateForUser(long userId) {
        WikiTagsResponse interests = wikiClient.getTags(userId);
        if (interests.tags() == null || interests.tags().isEmpty()) {
            // 관심사 0개면 종합할 게 없다. 프론트는 버튼 비활성화가 1차 가드이고, 서버는 표준 코드로 방어한다
            // (프론트 constants/errors.ts 가 알려진 코드만 매핑 → VALIDATION_ERROR 로 통일, message 는 미노출).
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "생성할 관심사가 없습니다.");
        }
        GenerationRequest request = new GenerationRequest(
                onDemandKey(userId), topic, contentType, null, null);
        String jobId = generationClient.requestGeneration(userId, request);
        log.info("[OnDemandGeneration] 즉시 생성 요청 userId={}, interests={}, idempotencyKey={}, jobId={}",
                userId, interests.tags().size(), request.idempotencyKey(), jobId);
        return GenerationTriggerResponse.accepted(jobId);
    }

    /** on-demand 멱등키(분 단위) — 연타는 1건, 시간 지나면 새 생성. */
    private String onDemandKey(long userId) {
        long minute = OffsetDateTime.now(KST).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
        return "ondemand-" + userId + "-" + contentType + "-" + minute;
    }
}
