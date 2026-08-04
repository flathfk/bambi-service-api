package com.bambi.service.agent.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 실패·프로세스 중단으로 남은 사용자 컨텍스트 Outbox를 주기적으로 재전송한다. */
@Component
@ConditionalOnProperty(name = "app.agent.context-outbox.enabled", havingValue = "true", matchIfMissing = true)
public class AgentContextOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentContextOutboxWorker.class);

    private final AgentContextOutboxDispatcher dispatcher;
    private final int batchSize;

    public AgentContextOutboxWorker(
            AgentContextOutboxDispatcher dispatcher,
            @Value("${app.agent.context-outbox.batch-size:50}") int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Context Outbox batch-size는 1 이상이어야 합니다.");
        }
        this.dispatcher = dispatcher;
        this.batchSize = batchSize;
    }

    /** 직전 폴링이 끝난 뒤 지정 간격만큼 기다려 중첩 없이 재시도한다. */
    @Scheduled(fixedDelayString = "${app.agent.context-outbox.poll-interval-ms:5000}",
            initialDelayString = "${app.agent.context-outbox.initial-delay-ms:5000}")
    public void poll() {
        try {
            dispatcher.dispatchBatch(batchSize);
        } catch (Exception e) {
            log.warn("[ContextOutbox] claim 실패 — 다음 주기 재시도", e);
        }
    }
}
