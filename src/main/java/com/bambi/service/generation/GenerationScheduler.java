package com.bambi.service.generation;

import com.bambi.service.briefing.BriefingTopicService;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 콘텐츠 생성 트리거 스케줄러 (송우 가이드 §3.4 "사용자 지정 시간 스케줄 = service 책임").
 * 지정 시각에 사용자별 생성 Job 을 agent 에 요청한다(정시 호출 방식 — scheduled_at 은 생략).
 *
 * <p>멱등키 = {날짜윈도우}-{userId}-{contentType} (예: 2026-07-30-23-interest_news_card)
 * → 스케줄러 재시도·중복 실행에도 agent 가 Job 을 한 번만 만든다.
 *
 * <p>기본 비활성(app.scheduler.generation.enabled=true 로 켬). 클라이언트 구현은
 * app.agent.generation.mode(mock|http)로 고르며, http 면 {@link RestClientGenerationClient} 가 실제로 호출한다.
 *
 * <p>대상은 활성 사용자 전원이다 — 온보딩에서 고른 관심사가 위키에 편입돼 가입자는 위키를 갖기 때문
 * (2026-08-04 송우 확인, 편입 로직은 agent 쪽에서 보강). 혹시 컨텍스트가 없는 사용자는 agent 가
 * 409/USER_CONTEXT_REQUIRED 를 내는데, 아래 per-user try/catch 가 그 사용자만 건너뛴다.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.generation.enabled", havingValue = "true")
public class GenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(GenerationScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 카드 제목용 고정 문구 (2026-08-07 계약). {@code topics} 를 함께 보낼 때만 쓴다 —
     * {@code topics} 가 비면 이 문구가 <b>실제 검색어로 되살아나</b> 엉뚱한 기사를 물어온다
     * (2026-08-05 유림 확인). 그래서 아래 루프가 빈 목록을 먼저 걸러낸다.
     */
    static final String TITLE_TOPIC = "오늘의 관심사 브리핑";

    private final GenerationClient generationClient;
    private final UserRepository userRepository;
    private final BriefingTopicService briefingTopicService;
    private final GenerationPendingService pendingService;
    private final String contentType;

    public GenerationScheduler(
            GenerationClient generationClient,
            UserRepository userRepository,
            BriefingTopicService briefingTopicService,
            GenerationPendingService pendingService,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        this.generationClient = generationClient;
        this.userRepository = userRepository;
        this.briefingTopicService = briefingTopicService;
        this.pendingService = pendingService;
        this.contentType = contentType;
    }

    /** 매일 지정 시각(기본 07:00 KST)에 실행. 한 사용자 실패가 나머지를 막지 않는다. */
    @Scheduled(cron = "${app.scheduler.generation.cron:0 0 7 * * *}", zone = "Asia/Seoul")
    public void triggerDailyGeneration() {
        LocalDate window = LocalDate.now(KST);
        List<Long> userIds = userRepository.findAllActiveIds();
        log.info("[GenerationScheduler] 생성 트리거 시작 window={}, users={}", window, userIds.size());

        int requested = 0;
        int skipped = 0;
        for (Long userId : userIds) {
            try {
                // 본문 주제 = 사용자가 미리 고른 것 → 없으면 등록 관심사(폴백 3단계, agent-api #20).
                List<String> topics = briefingTopicService.resolveForMorningBriefing(userId);
                if (topics.isEmpty()) {
                    // 🚨 여기서 안 걸르면 TITLE_TOPIC 고정 문구가 실제 검색어로 되살아나
                    // 엉뚱한 기사를 물어온다(유림 확인 08-05). 보낼 주제가 없으면 안 보낸다.
                    skipped++;
                    continue;
                }
                GenerationRequest request = GenerationRequest.multiTopic(
                        idempotencyKey(window, userId, contentType),
                        TITLE_TOPIC,   // topics 가 있으면 topic 은 검색어가 아니라 카드 제목용이다
                        topics,
                        contentType,
                        GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING);  // agent 가 snapshot 에 에코
                String agentJobId = generationClient.requestGeneration(userId, request);
                // 접수 성공 후 펜딩 영속화(MORNING_BRIEFING) — 온디맨드와 같은 결정적 id 규칙.
                // 재실행·재시도는 같은 멱등키 → 같은 id 라 중복 행이 생기지 않는다.
                //
                // ⚠️ 슬롯 제목은 TITLE_TOPIC 이 아니라 topics[0] 이다. 고정 문구를 저장하면
                // 모든 사용자의 "처리중" 슬롯이 같은 문구가 되어 무엇을 만드는 중인지 안 보인다.
                pendingService.register(userId, request.idempotencyKey(),
                        GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING,
                        topics.get(0), contentType, agentJobId);
                requested++;
            } catch (Exception e) {
                // agent 다운/일부 실패는 전체를 막지 않는다(컨텍스트 동기화 패턴과 동일).
                log.warn("[GenerationScheduler] 사용자 생성 요청 실패 userId={} — 건너뜀", userId, e);
            }
        }
        log.info("[GenerationScheduler] 생성 트리거 완료 요청={}/{} (보낼 주제 없어 건너뜀={})",
                requested, userIds.size(), skipped);
    }

    /**
     * 멱등키 = {날짜윈도우}-{userId}-{contentType}.
     * 같은 날·같은 사용자·같은 유형은 항상 같은 키 → agent Job 중복 생성 방지.
     */
    static String idempotencyKey(LocalDate window, long userId, String contentType) {
        return window + "-" + userId + "-" + contentType;
    }
}
