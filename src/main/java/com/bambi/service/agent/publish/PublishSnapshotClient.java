package com.bambi.service.agent.publish;

import com.bambi.service.agent.publish.dto.AckRequest;
import com.bambi.service.agent.publish.dto.ClaimRequest;
import com.bambi.service.agent.publish.dto.ClaimResponse;

/**
 * 발행 스냅샷 배치 Pull 경계 (docs/service-integration-guide.md §4).
 * agent-api 는 service 를 호출하지 않으므로, service-worker 가 이 클라이언트로
 * 완성 콘텐츠를 폴링(claim)해서 가져오고 처리 후 ack 한다.
 *
 * P0(이번 주)는 {@link MockPublishSnapshotClient} 인프로세스 큐로 관통하고,
 * P1 에서 실제 agent-api HTTP 호출(POST /internal/v1/publish-snapshot-batches/*)로
 * 교체한다. 워커 코드는 이 인터페이스만 알면 되므로 교체 시 무변경.
 */
public interface PublishSnapshotClient {

    /** 발행 대기 배치를 lease 걸고 가져온다. 없으면 items=[]. */
    ClaimResponse claim(ClaimRequest request);

    /** 처리 결과를 부분 성공 ACK. ack 하지 않은 항목은 lease 만료 후 재-claim 대상. */
    void ack(String batchId, AckRequest request);
}
