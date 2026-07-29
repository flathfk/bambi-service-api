package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * P0 Mock — 실제 agent 를 호출하지 않고 생성 요청을 로그로만 남긴다.
 * 스케줄러 구조·멱등키·예약 시각 규약을 검증하기 위한 인프로세스 스텁.
 * P1 에서 실제 HTTP 호출 구현체로 교체(그때 Profile 격리).
 */
@Component
public class MockGenerationClient implements GenerationClient {

    private static final Logger log = LoggerFactory.getLogger(MockGenerationClient.class);

    @Override
    public void requestGeneration(long userId, GenerationRequest request) {
        log.info("[MockGeneration] 생성 요청 userId={}, idempotencyKey={}, contentType={}, scheduledAt={}",
                userId, request.idempotencyKey(), request.contentType(), request.scheduledAt());
    }
}
