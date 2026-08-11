package com.bambi.service.generation.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 스케줄 Outbox 이벤트의 짧은 Claim 트랜잭션과 소유권 기반 종결을 관리한다. */
@Service
public class GenerationScheduleOutboxService {

    private final GenerationScheduleOutboxRepository repository;

    public GenerationScheduleOutboxService(GenerationScheduleOutboxRepository repository) {
        this.repository = repository;
    }

    /** 처리 가능한 이벤트를 Batch로 잠근 뒤 lease와 Worker 소유자를 기록한다. */
    @Transactional
    public List<GenerationScheduleOutboxEvent> claim(
            int limit,
            String workerId,
            int leaseSeconds) {
        List<GenerationScheduleOutboxEvent> claimed = repository.findClaimable(limit);
        claimed.forEach(event -> event.markProcessing(workerId, leaseSeconds));
        return List.copyOf(claimed);
    }

    /** 아직 같은 Worker가 소유한 이벤트만 전달 완료로 닫는다. */
    @Transactional
    public boolean markDelivered(long id, String workerId, String agentJobId) {
        return repository.findById(id)
                .filter(event -> event.isOwnedBy(workerId))
                .map(event -> {
                    event.markDelivered(agentJobId);
                    return true;
                })
                .orElse(false);
    }

    /** 아직 같은 Worker가 소유한 실패 이벤트만 재시도 또는 DEAD 상태로 전환한다. */
    @Transactional
    public boolean markRetry(
            long id,
            String workerId,
            Throwable error,
            int maxAttempts,
            int retryBaseSeconds,
            int retryMaxSeconds) {
        return repository.findById(id)
                .filter(event -> event.isOwnedBy(workerId))
                .map(event -> {
                    event.markRetry(error, maxAttempts, retryBaseSeconds, retryMaxSeconds);
                    return true;
                })
                .orElse(false);
    }
}
