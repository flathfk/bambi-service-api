package com.bambi.service.generation;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.generation.dto.GenerationTriggerResponse;
import com.bambi.service.wiki.AgentWikiClient;
import com.bambi.service.wiki.dto.WikiTag;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OnDemandGenerationService} — 관심사 종합 즉시 생성: 관심사 있으면 jobId 반환, 없으면 NO_INTEREST.
 */
class OnDemandGenerationServiceTest {

    private final GenerationClient generationClient = mock(GenerationClient.class);
    private final AgentWikiClient wikiClient = mock(AgentWikiClient.class);
    private final OnDemandGenerationService service =
            new OnDemandGenerationService(generationClient, wikiClient, "내 관심사 종합 브리핑", "interest_news_card");

    private static WikiTagsResponse tagsWith(String... names) {
        List<WikiTag> tags = java.util.Arrays.stream(names)
                .map(n -> new WikiTag("id-" + n, n, "organization", 1.0, 0.7, List.of(), java.util.Map.of()))
                .toList();
        return new WikiTagsResponse("prof", 1, "active", null, tags);
    }

    @Test
    @DisplayName("관심사가 있으면 종합 생성을 접수하고 jobId 를 반환한다")
    void triggersWithInterestsAndReturnsJobId() {
        when(wikiClient.getTags(28L)).thenReturn(tagsWith("SK하이닉스", "삼성전자"));
        when(generationClient.requestGeneration(eq(28L), any())).thenReturn("job-99");

        GenerationTriggerResponse response = service.generateForUser(28L);

        assertThat(response.status()).isEqualTo("accepted");
        assertThat(response.jobId()).isEqualTo("job-99");
        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generationClient).requestGeneration(eq(28L), captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("내 관심사 종합 브리핑");
        assertThat(captor.getValue().idempotencyKey()).startsWith("ondemand-28-interest_news_card-");
    }

    @Test
    @DisplayName("관심사가 없으면 VALIDATION_ERROR 로 막고 생성하지 않는다")
    void noInterestRejects() {
        when(wikiClient.getTags(28L)).thenReturn(WikiTagsResponse.empty());

        assertThatThrownBy(() -> service.generateForUser(28L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(generationClient, never()).requestGeneration(any(Long.class), any());
    }
}
