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
                acks.add(AckRequest.AckItem.published(
                        item.contentId(), item.version(), item.snapshotHash()));
            } catch (Exception e) {
                // 실패 분류: 잘못된 페이로드(비숫자 user_id 등)는 재전달해도 계속 실패 → 영구(retryable=false).
                // DB 일시 장애 등은 재시도(retryable=true). 무한 재시도 루프 방지.
                boolean retryable = isRetryable(e);
                log.warn("[PublishWorker] 항목 처리 실패 contentId={} — {} ACK",
                        item.contentId(), retryable ? "retryable" : "permanent", e);
                acks.add(AckRequest.AckItem.failed(
                        item.contentId(), item.version(), item.snapshotHash(), retryable, failureReason(e)));
            }
        }

        try {
            publishClient.ack(claimed.batchId(), new AckRequest(workerId, acks));
        } catch (Exception e) {
            // ACK 실패해도 lease 만료 후 재-claim 되므로 유실 없음.
            log.warn("[PublishWorker] ack 실패 batchId={} — lease 만료 후 재처리", claimed.batchId(), e);
        }
    }

    /**
     * 재시도 가능한 실패인가.
     * 잘못된 페이로드(비숫자 user_id → IllegalArgumentException 등)는 재전달해도 계속 실패하므로
     * 영구 실패(false)로 ACK 해 agent 가 재-발행하지 않게 한다(무한 루프 방지).
     * 그 외(DB 일시 장애 등)는 재시도(true) → Backoff 후 재-claim.
     */
    private static boolean isRetryable(Exception e) {
        return !(e instanceof IllegalArgumentException);
    }

    /**
     * agent 로 보낼 실패 사유. 예외 메시지에는 SQL·접속정보가 섞일 수 있어 예외 타입만 넘긴다
     * (전체 스택은 위 log.warn 으로 우리 로그에 남는다).
     */
    private static String failureReason(Exception e) {
        return e.getClass().getSimpleName();
    }
}
