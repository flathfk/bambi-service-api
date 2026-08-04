package com.bambi.service.agent.outbox;

import com.bambi.service.agent.dto.AgentContextRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbox 적재·claim·상태 전이를 각각 짧은 DB 트랜잭션으로 수행한다. */
@Service
public class AgentContextOutboxStore {

    private final AgentContextOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public AgentContextOutboxStore(AgentContextOutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** 호출한 비즈니스 트랜잭션에 동일 버전·동일 payload의 PENDING 행을 적재한다. */
    @Transactional
    public void enqueue(long userId, AgentContextRequest request) {
        AgentContextOutbox outbox = new AgentContextOutbox(
                userId, request.contextVersion(), serialize(request), OffsetDateTime.now());
        repository.save(outbox);
    }

    /** 처리 가능한 Outbox 행을 잠그고 lease를 설정한 뒤 불변 claim 목록을 반환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedAgentContext> claimBatch(String workerId, int batchSize, Duration leaseDuration) {
        OffsetDateTime now = OffsetDateTime.now();
        return repository.lockClaimableBatch(now, batchSize).stream()
                .map(outbox -> claim(outbox, workerId, now, leaseDuration))
                .toList();
    }

    /** 특정 사용자의 가장 오래된 처리 가능 행 하나를 claim한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedAgentContext> claimForUser(long userId, String workerId, Duration leaseDuration) {
        OffsetDateTime now = OffsetDateTime.now();
        return repository.lockClaimableForUser(userId, now).stream()
                .findFirst()
                .map(outbox -> claim(outbox, workerId, now, leaseDuration));
    }

    /** claim token이 여전히 유효할 때만 전달 완료로 전환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markPublished(long outboxId, UUID lockToken) {
        AgentContextOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);
        if (outbox == null || !outbox.isClaimedBy(lockToken)) {
            return false;
        }
        outbox.markPublished(OffsetDateTime.now());
        return true;
    }

    /** claim token이 여전히 유효할 때만 backoff를 적용해 재시도 대기로 돌린다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean scheduleRetry(long outboxId, UUID lockToken, Duration retryDelay, String failureCode) {
        AgentContextOutbox outbox = repository.findByIdForUpdate(outboxId).orElse(null);
        if (outbox == null || !outbox.isClaimedBy(lockToken)) {
            return false;
        }
        outbox.scheduleRetry(OffsetDateTime.now(), retryDelay, failureCode);
        return true;
    }

    private ClaimedAgentContext claim(AgentContextOutbox outbox, String workerId,
                                      OffsetDateTime now, Duration leaseDuration) {
        UUID token = outbox.claim(workerId, now, leaseDuration);
        return new ClaimedAgentContext(
                outbox.getId(), outbox.getUserId(), outbox.getPayload(), outbox.getAttemptCount(), token);
    }

    private String serialize(AgentContextRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Agent 사용자 컨텍스트 Outbox 직렬화 실패", e);
        }
    }
}
