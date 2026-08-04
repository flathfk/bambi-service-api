package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock — 실제 agent 를 호출하지 않고 생성 요청을 로그로만 남긴다.
 * 스케줄러 구조·멱등키·예약 시각 규약을 검증하기 위한 인프로세스 스텁.
 *
 * <p>{@code app.agent.generation.mode=mock}(미설정 기본)일 때만 등록된다.
 * {@code http}면 대신 {@link RestClientGenerationClient}(실제 agent-api 호출)가 뜬다.
 * 두 구현이 같은 {@link GenerationClient} 빈이라 조건으로 하나만 올려 충돌을 막는다.
 */
@Component
@ConditionalOnProperty(name = "app.agent.generation.mode", havingValue = "mock", matchIfMissing = true)
public class MockGenerationClient implements GenerationClient {

    private static final Logger log = LoggerFactory.getLogger(MockGenerationClient.class);

    @Override
    public void requestGeneration(long userId, GenerationRequest request) {
        log.info("[MockGeneration] 생성 요청 userId={}, idempotencyKey={}, contentType={}, scheduledAt={}",
                userId, request.idempotencyKey(), request.contentType(), request.scheduledAt());
    }
}
