package com.bambi.service.generation;

import com.bambi.service.briefing.BriefingTopicService;
import com.bambi.service.generation.dto.GenerationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 한 명의 아침 브리핑 주제를 확정하고 공통 생성 접수 경로로 제출한다.
 *
 * <p>정기 스케줄러와 개발용 즉시 생성 API가 이 서비스를 함께 사용한다. 호출 경로가 달라도
 * Wiki 문맥 선정·등록 관심사 폴백·다중 주제 요청 조립 규칙이 달라지지 않게 하는 경계다.
 */
@Service
public class MorningBriefingGenerationService {

    /** {@code topics}가 있을 때만 쓰는 카드 제목용 문구. */
    static final String TITLE_TOPIC = "오늘의 관심사 브리핑";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GenerationSubmissionService submissionService;
    private final BriefingTopicService briefingTopicService;
    private final String contentType;

    public MorningBriefingGenerationService(
            GenerationSubmissionService submissionService,
            BriefingTopicService briefingTopicService,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        this.submissionService = submissionService;
        this.briefingTopicService = briefingTopicService;
        this.contentType = contentType;
    }

    /**
     * 아침 브리핑을 즉시 접수한다. 생성할 주제가 없으면 Agent에 고정 제목을 검색어로 보내지 않고
     * 빈 결과를 반환한다. 멱등키의 시간 창은 스케줄러·개발 API 등 호출자가 소유한다.
     */
    public Optional<GenerationSubmissionService.Submission> submit(
            long userId,
            String idempotencyKey) {
        return submit(userId, idempotencyKey, LocalDate.now(KST));
    }

    /** 지정 날짜의 준비 Snapshot을 조회해 같은 날짜를 고정한 아침 브리핑을 접수한다. */
    public Optional<GenerationSubmissionService.Submission> submit(
            long userId,
            String idempotencyKey,
            LocalDate briefingDate) {
        List<String> topics = briefingTopicService.resolveForMorningBriefing(userId, briefingDate);
        if (topics.isEmpty()) {
            return Optional.empty();
        }

        GenerationRequest request = GenerationRequest.morningBriefing(
                idempotencyKey,
                TITLE_TOPIC,
                topics,
                contentType,
                GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING,
                briefingDate);
        return Optional.of(submissionService.submit(
                userId,
                request,
                GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING,
                topics.get(0)));
    }
}
