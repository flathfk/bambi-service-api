package com.bambi.service.agent.outbox;

import com.bambi.service.agent.AgentGateway;
import com.bambi.service.agent.dto.AgentContextRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Claim한 Outbox payload를 Agent API에 보내고 성공 또는 재시도 상태를 기록한다. */
@Component
public class AgentContextOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AgentContextOutboxDispatcher.class);

    private final AgentContextOutboxStore outboxStore;
    private final AgentGateway agentGateway;
    private final ObjectMapper objectMapper;
    private final String workerId;
    private final Duration leaseDuration;
    private final Duration initialRetryDelay;
    private final Duration maxRetryDelay;

    public AgentContextOutboxDispatcher(
            AgentContextOutboxStore outboxStore,
            AgentGateway agentGateway,
            ObjectMapper objectMapper,
            @Value("${app.agent.context-outbox.worker-id:}") String configuredWorkerId,
            @Value("${app.agent.context-outbox.lease-seconds:30}") long leaseSeconds,
            @Value("${app.agent.context-outbox.retry-initial-seconds:5}") long initialRetrySeconds,
            @Value("${app.agent.context-outbox.retry-max-seconds:3600}") long maxRetrySeconds) {
        if (leaseSeconds <= 0 || initialRetrySeconds <= 0 || maxRetrySeconds < initialRetrySeconds) {
            throw new IllegalArgumentException("Context Outbox 시간 설정은 양수이고 retry-max는 initial 이상이어야 합니다.");
        }
        this.outboxStore = outboxStore;
        this.agentGateway = agentGateway;
        this.objectMapper = objectMapper;
        this.workerId = configuredWorkerId == null || configuredWorkerId.isBlank()
                ? "context-outbox-" + UUID.randomUUID()
                : configuredWorkerId;
        if (this.workerId.length() > 100) {
            throw new IllegalArgumentException("Context Outbox worker-id는 100자 이하여야 합니다.");
        }
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
        this.initialRetryDelay = Duration.ofSeconds(initialRetrySeconds);
        this.maxRetryDelay = Duration.ofSeconds(maxRetrySeconds);
    }

    /** 가입·설정 변경 커밋 직후 해당 사용자의 Outbox를 지연 없이 한 번 전달한다. */
    public void dispatchForUser(long userId) {
        outboxStore.claimForUser(userId, workerId, leaseDuration).ifPresent(this::deliver);
    }

    /** due 또는 lease 만료 행을 최대 batchSize개 claim해 각각 독립적으로 전달한다. */
    public int dispatchBatch(int batchSize) {
        List<ClaimedAgentContext> claimed = outboxStore.claimBatch(workerId, batchSize, leaseDuration);
        claimed.forEach(this::deliver);
        return claimed.size();
    }

    private void deliver(ClaimedAgentContext claimed) {
        try {
            AgentContextRequest request = objectMapper.readValue(claimed.payload(), AgentContextRequest.class);
            agentGateway.syncUserContext(claimed.userId(), request);
            if (!outboxStore.markPublished(claimed.outboxId(), claimed.lockToken())) {
                log.warn("[ContextOutbox] 완료 기록 생략 — lease 소유권 상실 outboxId={}", claimed.outboxId());
            }
        } catch (Exception e) {
            Duration retryDelay = retryDelay(claimed.attemptCount());
            boolean scheduled = false;
            try {
                scheduled = outboxStore.scheduleRetry(
                        claimed.outboxId(), claimed.lockToken(), retryDelay, failureCode(e));
            } catch (Exception stateError) {
                log.error("[ContextOutbox] 재시도 상태 기록 실패 outboxId={}", claimed.outboxId(), stateError);
            }
            log.warn("[ContextOutbox] Agent 동기화 실패 outboxId={}, userId={}, attempt={}, retryIn={}s, scheduled={}",
                    claimed.outboxId(), claimed.userId(), claimed.attemptCount(), retryDelay.toSeconds(), scheduled, e);
        }
    }

    /** 첫 실패는 initial, 이후 2배씩 늘리되 max에서 제한한다. */
    Duration retryDelay(int attemptCount) {
        long delay = initialRetryDelay.toSeconds();
        long max = maxRetryDelay.toSeconds();
        for (int attempt = 1; attempt < attemptCount && delay < max; attempt++) {
            delay = Math.min(max, delay > max / 2 ? max : delay * 2);
        }
        return Duration.ofSeconds(delay);
    }

    private static String failureCode(Exception e) {
        String simpleName = e.getClass().getSimpleName();
        return simpleName.length() <= 100 ? simpleName : simpleName.substring(0, 100);
    }
}
