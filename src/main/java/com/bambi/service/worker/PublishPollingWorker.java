package com.bambi.service.worker;

import com.bambi.service.agent.publish.PublishSnapshotClient;
import com.bambi.service.agent.publish.dto.AckRequest;
import com.bambi.service.agent.publish.dto.ClaimRequest;
import com.bambi.service.agent.publish.dto.ClaimResponse;
import com.bambi.service.agent.publish.dto.PublishItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * service-worker 발행 폴링 루프 (docs/service-integration-guide.md §4).
 * 주기적으로 claim → 항목별 멱등 Upsert → 부분 성공 ACK.
 * 생성 완료 콘텐츠를 service-db 로 옮기는 유일한 경로.
 *
 * MVP 는 단일 인스턴스 전제(분산 락 없음) — 다중 인스턴스/리더 선출은 P1(Redis).
 * app.worker.publish.enabled=false 로 끌 수 있다(테스트/단일 배포 제어용).
 */
@Component
@ConditionalOnProperty(name = "app.worker.publish.enabled", havingValue = "true", matchIfMissing = true)
public class PublishPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(PublishPollingWorker.class);

    private final PublishSnapshotClient publishClient;
    private final PublishProcessingService processingService;

    @Value("${app.worker.publish.worker-id:service-worker-01}")
    private String workerId;

    @Value("${app.worker.publish.batch-limit:50}")
    private int batchLimit;

    @Value("${app.worker.publish.lease-seconds:120}")
    private int leaseSeconds;

    public PublishPollingWorker(PublishSnapshotClient publishClient,
                                PublishProcessingService processingService) {
        this.publishClient = publishClient;
        this.processingService = processingService;
    }

    /** fixedDelay: 직전 실행 종료 후 대기 → 처리 지연 시에도 중첩 실행 안 함. */
    @Scheduled(fixedDelayString = "${app.worker.publish.poll-interval-ms:15000}",
            initialDelayString = "${app.worker.publish.initial-delay-ms:10000}")
    public void poll() {
        ClaimResponse claimed;
        try {
            claimed = publishClient.claim(new ClaimRequest(workerId, batchLimit, leaseSeconds));
        } catch (Exception e) {
            log.warn("[PublishWorker] claim 실패 — 다음 주기 재시도", e);
            return;
        }
        if (claimed.isEmpty()) {
            return;   // 처리할 것 없음 — 조용히 대기
        }

        List<AckRequest.AckItem> acks = new ArrayList<>();
        for (PublishItem item : claimed.items()) {
            try {
                processingService.upsert(item);   // 항목별 독립 트랜잭션
                acks.add(AckRequest.AckItem.published(item.contentId(), item.snapshotHash()));
            } catch (Exception e) {
                // 실패는 retryable 로 ACK → Backoff 후 ready 복귀. (영구 실패 분류는 P1)
                log.warn("[PublishWorker] 항목 처리 실패 contentId={} — retryable ACK", item.contentId(), e);
                acks.add(AckRequest.AckItem.failed(item.contentId(), item.snapshotHash(), true));
            }
        }

        try {
            publishClient.ack(claimed.batchId(), new AckRequest(workerId, acks));
        } catch (Exception e) {
            // ACK 실패해도 lease 만료 후 재-claim 되므로 유실 없음.
            log.warn("[PublishWorker] ack 실패 batchId={} — lease 만료 후 재처리", claimed.batchId(), e);
        }
    }
}
