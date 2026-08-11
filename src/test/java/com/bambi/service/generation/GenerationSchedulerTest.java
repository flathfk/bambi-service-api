package com.bambi.service.generation;

import com.bambi.service.briefing.BriefingTopicService;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GenerationScheduler} — 멱등키 규약 + topics[] 전송 + 부분 실패 격리.
 *
 * <p>2026-08-07 계약 이후 아침 브리핑은 <b>주제 여러 개</b>를 한 장에 묶는다. {@code topic} 은
 * 검색어가 아니라 카드 제목용 고정 문구가 되고, 본문 주제는 {@code topics} 가 정한다.
 * 그래서 여기서 지키는 것은 <b>topics 가 비면 아예 안 보낸다</b>는 규칙이다 — 안 지키면
 * 고정 문구가 검색어로 되살아나 엉뚱한 기사를 물어온다(2026-08-05 유림 확인).
 */
class GenerationSchedulerTest {

    private final GenerationClient client = mock(GenerationClient.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BriefingTopicService briefingTopicService = mock(BriefingTopicService.class);
    // 펜딩 접수 레이어 — 실제 구현 + repo mock (기록 실패 삼킴 정책까지 그대로 태운다).
    private final GenerationPendingRepository pendingRepository = mock(GenerationPendingRepository.class);
    private final GenerationPendingService pendingService = new GenerationPendingService(pendingRepository);
    private final GenerationSubmissionService submissionService =
            new GenerationSubmissionService(client, pendingService);
    private final MorningBriefingGenerationService morningBriefingGenerationService =
            new MorningBriefingGenerationService(
                    submissionService, briefingTopicService, "interest_news_card");
    private final GenerationScheduler scheduler =
            new GenerationScheduler(
                    morningBriefingGenerationService, userRepository, "interest_news_card");

    @Test
    void 멱등키는_날짜윈도우_userId_contentType_규약이다() {
        String key = GenerationScheduler.idempotencyKey(
                LocalDate.of(2026, 7, 30), 23L, "interest_news_card");

        // 송우 가이드 예시와 동일한 형태. 흔들리는 값(선택 주제 등)은 키에 넣지 않는다 —
        // 두 호출 사이에 값이 바뀌면 같은 날 두 건이 발행된다.
        assertThat(key).isEqualTo("2026-07-30-23-interest_news_card");
    }

    @Test
    void 사용자별_주제를_topics_로_보내고_topic_은_제목용_고정문구다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L, 3L));
        when(briefingTopicService.resolveForMorningBriefing(anyLong()))
                .thenReturn(List.of("폭염", "퇴근", "웹툰·애니"));

        scheduler.triggerDailyGeneration();

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(client, times(3)).requestGeneration(anyLong(), captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(r -> "interest_news_card".equals(r.contentType()))
                // topics 가 있으면 topic 은 검색어가 아니라 카드 제목용이다
                .allMatch(r -> MorningBriefingGenerationService.TITLE_TOPIC.equals(r.topic()))
                .allMatch(r -> List.of("폭염", "퇴근", "웹툰·애니").equals(r.topics()))
                .anyMatch(r -> r.idempotencyKey().endsWith("-1-interest_news_card"));
    }

    @Test
    void 주제_순서를_그대로_보낸다() {
        // agent 계약상 topics 순서가 곧 리포트 안 섹션 순서다 — 흔들면 안 된다.
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L));
        when(briefingTopicService.resolveForMorningBriefing(1L))
                .thenReturn(List.of("퇴근", "폭염", "웹툰·애니"));

        scheduler.triggerDailyGeneration();

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(client).requestGeneration(anyLong(), captor.capture());
        assertThat(captor.getValue().topics()).containsExactly("퇴근", "폭염", "웹툰·애니");
    }

    @Test
    void 펜딩_슬롯_제목은_고정문구가_아니라_첫_주제다() {
        // 고정 문구를 저장하면 모든 사용자의 "처리중" 슬롯이 같은 문구가 되어
        // 무엇을 만드는 중인지 안 보인다.
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L));
        when(briefingTopicService.resolveForMorningBriefing(1L)).thenReturn(List.of("폭염", "퇴근"));

        scheduler.triggerDailyGeneration();

        verify(pendingRepository).insertPending(
                any(), anyLong(), any(),
                eq(GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING),
                eq("폭염"), eq("interest_news_card"), any());
    }

    @Test
    void 보낼_주제가_없으면_요청을_아예_안_보낸다() {
        // 🚨 여기서 안 걸르면 TITLE_TOPIC 이 실제 검색어로 되살아나 엉뚱한 기사를 물어온다.
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L));
        when(briefingTopicService.resolveForMorningBriefing(1L)).thenReturn(List.of("폭염"));
        when(briefingTopicService.resolveForMorningBriefing(2L)).thenReturn(List.of());

        scheduler.triggerDailyGeneration();

        verify(client, times(1)).requestGeneration(eq(1L), any());
        verify(client, never()).requestGeneration(eq(2L), any());
    }

    @Test
    void 한_사용자_실패가_나머지를_막지_않는다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L, 3L));
        when(briefingTopicService.resolveForMorningBriefing(anyLong())).thenReturn(List.of("폭염"));
        doThrow(new RuntimeException("agent down")).when(client).requestGeneration(eq(2L), any());

        scheduler.triggerDailyGeneration();

        // 2번이 터져도 1·3번은 요청됨 (총 3번 호출 시도)
        verify(client, times(3)).requestGeneration(anyLong(), any());
        // 실패한 2번은 접수가 안 됐으니 펜딩도 안 남는다 (1·3번만 기록)
        verify(pendingRepository, times(2)).insertPending(any(), anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void 주제_조회가_터진_사용자도_나머지를_막지_않는다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L));
        when(briefingTopicService.resolveForMorningBriefing(1L))
                .thenThrow(new RuntimeException("db down"));
        when(briefingTopicService.resolveForMorningBriefing(2L)).thenReturn(List.of("폭염"));

        scheduler.triggerDailyGeneration();

        verify(client, never()).requestGeneration(eq(1L), any());
        verify(client, times(1)).requestGeneration(eq(2L), any());
    }
}
