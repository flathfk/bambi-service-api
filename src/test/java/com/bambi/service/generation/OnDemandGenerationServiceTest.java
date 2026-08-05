package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link OnDemandGenerationService} — 즉시 생성이 요청 topic 을 우선하고, 없으면 기본값으로 트리거하는지 검증.
 */
class OnDemandGenerationServiceTest {

    private final GenerationClient generationClient = mock(GenerationClient.class);
    private final OnDemandGenerationService service =
            new OnDemandGenerationService(generationClient, "오늘의 관심사 뉴스", "interest_news_card");

    @Test
    @DisplayName("요청 topic 을 주면 그 주제로 생성한다")
    void usesRequestedTopic() {
        service.generateForUser(28L, "  SK하이닉스  ");

        GenerationRequest sent = capture();
        assertThat(sent.topic()).isEqualTo("SK하이닉스");   // strip 적용
        assertThat(sent.contentType()).isEqualTo("interest_news_card");
        assertThat(sent.idempotencyKey()).startsWith("ondemand-28-interest_news_card-");
    }

    @Test
    @DisplayName("topic 이 비면 서버 기본값으로 생성한다")
    void fallsBackToDefaultTopic() {
        service.generateForUser(28L, "   ");

        assertThat(capture().topic()).isEqualTo("오늘의 관심사 뉴스");
    }

    @Test
    @DisplayName("topic 이 null 이어도 기본값으로 생성한다")
    void nullTopicFallsBack() {
        service.generateForUser(28L, null);

        assertThat(capture().topic()).isEqualTo("오늘의 관심사 뉴스");
    }

    private GenerationRequest capture() {
        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generationClient).requestGeneration(eq(28L), captor.capture());
        return captor.getValue();
    }
}
