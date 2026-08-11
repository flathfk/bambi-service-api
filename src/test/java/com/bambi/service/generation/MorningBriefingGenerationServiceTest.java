package com.bambi.service.generation;

import com.bambi.service.briefing.BriefingTopicService;
import com.bambi.service.generation.dto.GenerationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link MorningBriefingGenerationService}의 준비 날짜 조회·생성 요청 전달을 검증한다. */
class MorningBriefingGenerationServiceTest {

    private final GenerationSubmissionService submissionService =
            mock(GenerationSubmissionService.class);
    private final BriefingTopicService briefingTopicService = mock(BriefingTopicService.class);
    private final MorningBriefingGenerationService service = new MorningBriefingGenerationService(
            submissionService, briefingTopicService, "interest_news_card");

    @Test
    void 조회한_Snapshot_날짜를_생성_요청에도_그대로_보낸다() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        when(briefingTopicService.resolveForMorningBriefing(7L, date))
                .thenReturn(List.of("반도체", "프로야구"));
        when(submissionService.submit(eq(7L), any(), any(), any()))
                .thenReturn(new GenerationSubmissionService.Submission("pending-7", "job-7"));

        var result = service.submit(7L, "morning-7", date);

        assertThat(result).isPresent();
        ArgumentCaptor<GenerationRequest> request = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(submissionService).submit(
                eq(7L), request.capture(),
                eq(GenerationPendingService.REPORT_TYPE_MORNING_BRIEFING), eq("반도체"));
        assertThat(request.getValue().briefingDate()).isEqualTo(date);
        assertThat(request.getValue().topics()).containsExactly("반도체", "프로야구");
    }

    @Test
    void 지정일에_주제가_없으면_Agent_생성을_접수하지_않는다() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        when(briefingTopicService.resolveForMorningBriefing(7L, date)).thenReturn(List.of());

        assertThat(service.submit(7L, "morning-7", date)).isEmpty();

        verify(submissionService, never()).submit(any(Long.class), any(), any(), any());
    }
}
