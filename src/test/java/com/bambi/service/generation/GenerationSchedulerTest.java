package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.user.UserRepository;
import com.bambi.service.wiki.AgentWikiClient;
import com.bambi.service.wiki.dto.WikiTag;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
 * {@link GenerationScheduler} — 멱등키 규약 + 사용자별 대표 관심사 검색 주제 + 부분 실패 격리.
 */
class GenerationSchedulerTest {

    private final GenerationClient client = mock(GenerationClient.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AgentWikiClient wikiClient = mock(AgentWikiClient.class);
    private final GenerationScheduler scheduler =
            new GenerationScheduler(client, userRepository, wikiClient, "interest_news_card");

    /** 대표 관심사 태그 하나짜리 프로필. */
    private static WikiTagsResponse profileWith(String tag) {
        return new WikiTagsResponse("prof", 1, "active", null,
                List.of(new WikiTag("id", tag, "topic", 0.9, 0.7, List.of(), Map.of())));
    }

    @Test
    void 멱등키는_날짜윈도우_userId_contentType_규약이다() {
        String key = GenerationScheduler.idempotencyKey(
                LocalDate.of(2026, 7, 30), 23L, "interest_news_card");

        // 송우 가이드 예시와 동일한 형태
        assertThat(key).isEqualTo("2026-07-30-23-interest_news_card");
    }

    @Test
    void 활성_사용자마다_대표관심사를_검색주제로_요청한다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L, 3L));
        when(wikiClient.getTags(anyLong())).thenReturn(profileWith("반도체"));

        scheduler.triggerDailyGeneration();

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(client, times(3)).requestGeneration(anyLong(), captor.capture());
        // content_type 기본값 + 멱등키에 userId 반영 + topic 은 고정값이 아니라 대표 관심사
        assertThat(captor.getAllValues())
                .allMatch(r -> "interest_news_card".equals(r.contentType()))
                .allMatch(r -> "반도체".equals(r.topic()))
                .anyMatch(r -> r.idempotencyKey().endsWith("-1-interest_news_card"));
    }

    @Test
    void 관심사_없는_사용자는_건너뛴다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L));
        when(wikiClient.getTags(1L)).thenReturn(profileWith("반도체"));
        when(wikiClient.getTags(2L)).thenReturn(WikiTagsResponse.empty());   // 관심사 0개

        scheduler.triggerDailyGeneration();

        // 관심사 있는 1번만 요청, 2번은 검색 주제가 없어 건너뜀
        verify(client, times(1)).requestGeneration(eq(1L), any());
        verify(client, never()).requestGeneration(eq(2L), any());
    }

    @Test
    void 한_사용자_실패가_나머지를_막지_않는다() {
        when(userRepository.findAllActiveIds()).thenReturn(List.of(1L, 2L, 3L));
        when(wikiClient.getTags(anyLong())).thenReturn(profileWith("반도체"));
        doThrow(new RuntimeException("agent down")).when(client).requestGeneration(eq(2L), any());

        scheduler.triggerDailyGeneration();

        // 2번이 터져도 1·3번은 요청됨 (총 3번 호출 시도)
        verify(client, times(3)).requestGeneration(anyLong(), any());
    }
}
