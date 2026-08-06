package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationPendingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 생성 접수(펜딩) 기록·조회 — 온디맨드/스케줄러 공용 접수 레이어 (2026-08-06 합의, 우석).
 *
 * <p>접수 기록은 agent 202 접수가 끝난 뒤에 남긴다(실패한 요청이 유령 펜딩을 만들지 않게).
 * 기록 실패는 접수 자체를 되돌리지 않는다 — agent Job 은 이미 등록됐으므로 warn 만 남긴다
 * (북마크 위키 중계와 같은 정책).
 *
 * <p>노출은 최근 {@link #VISIBLE_WINDOW} 안의 PENDING 만 — 완료 전환(claim 연결고리)이 붙기
 * 전까지 오래된 접수가 "처리중"으로 영영 남는 것을 시간 창으로 막는다(후속: 소라 협의).
 */
@Service
public class GenerationPendingService {

    /** 생성 유형 값 (2026-08-06 계약): 아침 정기 브리핑. */
    public static final String REPORT_TYPE_MORNING_BRIEFING = "MORNING_BRIEFING";
    /** 생성 유형 값 (2026-08-06 계약): 사용자 즉시 생성. */
    public static final String REPORT_TYPE_ON_DEMAND = "ON_DEMAND";

    private static final Logger log = LoggerFactory.getLogger(GenerationPendingService.class);
    private static final Duration VISIBLE_WINDOW = Duration.ofMinutes(60);

    private final GenerationPendingRepository pendingRepository;

    public GenerationPendingService(GenerationPendingRepository pendingRepository) {
        this.pendingRepository = pendingRepository;
    }

    /**
     * 접수 사실을 멱등 기록하고 펜딩 id 를 반환한다. id 는 멱등키 파생 결정적 UUID 라
     * 같은 접수(같은 분 연타·스케줄러 재시도)는 같은 id 로 모여 중복 행이 생기지 않는다.
     * 기록 실패는 삼킨다 — agent 접수는 이미 성공했으므로 트리거 응답을 막지 않는다.
     */
    public String register(long userId, String idempotencyKey, String reportType,
                           String topic, String contentType, String agentJobId) {
        UUID id = deterministicId(idempotencyKey);
        try {
            // 트랜잭션은 리포지토리 메서드(REQUIRES_NEW)가 직접 연다 — 서비스 자기호출은 프록시를 안 탄다.
            pendingRepository.insertPending(id, userId, idempotencyKey, reportType,
                    truncate(topic, 500), contentType, agentJobId);
        } catch (Exception e) {
            log.warn("[GenerationPending] 접수 기록 실패 (userId={}, key={}) — 접수는 유지",
                    userId, idempotencyKey, e);
        }
        return id.toString();
    }

    /** 본인 최근 60분 PENDING 목록 — 홈 "처리중" 슬롯용. */
    @Transactional(readOnly = true)
    public List<GenerationPendingResponse> listRecent(long userId) {
        OffsetDateTime after = OffsetDateTime.now().minus(VISIBLE_WINDOW);
        return pendingRepository
                .findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(userId, "PENDING", after)
                .stream()
                .map(GenerationPendingResponse::from)
                .toList();
    }

    /**
     * 펜딩 id — 멱등키 파생 결정적 UUID. 트리거 응답(GenerationTriggerResponse.id)과
     * 같은 규칙이라 접수 응답과 펜딩 목록을 프론트가 매칭할 수 있다(우석 08-05 키 설계).
     */
    public static UUID deterministicId(String idempotencyKey) {
        return UUID.nameUUIDFromBytes(idempotencyKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
