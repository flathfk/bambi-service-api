package com.bambi.service.onboarding;

import com.bambi.service.agent.AgentContextSyncService;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.generation.GenerationClient;
import com.bambi.service.generation.GenerationPendingService;
import com.bambi.service.generation.GenerationSubmissionService;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.interest.Interest;
import com.bambi.service.interest.InterestRepository;
import com.bambi.service.interest.InterestSource;
import com.bambi.service.onboarding.dto.OnboardingCompleteRequest;
import com.bambi.service.onboarding.dto.OnboardingCompleteResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 온보딩 완료의 순서 검증·컨텍스트 동기화·슬롯 멱등키·최대 3개 접수를 검증한다. */
class OnboardingCompletionServiceTest {

    private final InterestRepository interestRepository = mock(InterestRepository.class);
    private final AgentContextSyncService contextSyncService = mock(AgentContextSyncService.class);
    private final GenerationClient generationClient = mock(GenerationClient.class);
    private final GenerationPendingService pendingService = mock(GenerationPendingService.class);
    private final GenerationSubmissionService submissionService =
            new GenerationSubmissionService(generationClient, pendingService);
    private final OnboardingCompletionService service = new OnboardingCompletionService(
            interestRepository,
            contextSyncService,
            new OnboardingReportSelector(),
            submissionService,
            "interest_news_card");

    @Test
    void 선택순서로_context를_동기화하고_고정슬롯_최대세개를_접수한다() {
        Interest categoryFirst = taxonomy(1L, "A-첫째", "category-a", "a-1");
        Interest custom = custom(2L, "직접 입력");
        Interest categorySecond = taxonomy(3L, "A-둘째", "category-a", "a-2");
        Interest otherCategory = taxonomy(4L, "B-첫째", "category-b", "b-1");
        when(interestRepository.findByUserIdAndSourceAndDeletedAtIsNull(7L, InterestSource.USER))
                .thenReturn(List.of(otherCategory, categorySecond, custom, categoryFirst));
        when(generationClient.requestGeneration(eq(7L), any()))
                .thenReturn("job-1", "job-2", "job-3");
        when(pendingService.register(eq(7L), any(), any(), any(), any(), any()))
                .thenReturn("pending-1", "pending-2", "pending-3");

        OnboardingCompleteResponse response = service.complete(
                7L,
                new OnboardingCompleteRequest(List.of(1L, 2L, 3L, 4L)));

        verify(contextSyncService).syncUserContext(
                7L, List.of(categoryFirst, custom, categorySecond, otherCategory));
        ArgumentCaptor<GenerationRequest> requests = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generationClient, times(3)).requestGeneration(eq(7L), requests.capture());
        assertThat(requests.getAllValues())
                .extracting(GenerationRequest::topic)
                .containsExactly("직접 입력", "A-첫째", "B-첫째");
        assertThat(requests.getAllValues())
                .extracting(GenerationRequest::idempotencyKey)
                .containsExactly(
                        "onboarding:7:slot:1",
                        "onboarding:7:slot:2",
                        "onboarding:7:slot:3");
        assertThat(requests.getAllValues())
                .allMatch(request -> GenerationPendingService.REPORT_TYPE_ONBOARDING
                        .equals(request.reportType()));
        assertThat(response.reports()).hasSize(3);
    }

    @Test
    void 현재_활성관심사와_선택순서_id가_다르면_생성하지_않는다() {
        Interest interest = custom(1L, "직접 입력");
        when(interestRepository.findByUserIdAndSourceAndDeletedAtIsNull(7L, InterestSource.USER))
                .thenReturn(List.of(interest));

        assertThatThrownBy(() -> service.complete(
                7L,
                new OnboardingCompleteRequest(List.of(999L))))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(contextSyncService, never()).syncUserContext(anyLong(), any());
        verify(generationClient, never()).requestGeneration(anyLong(), any());
    }

    private static Interest custom(long id, String name) {
        Interest interest = new Interest(7L, name);
        ReflectionTestUtils.setField(interest, "id", id);
        return interest;
    }

    private static Interest taxonomy(
            long id,
            String name,
            String categoryId,
            String topicId) {
        Interest interest = Interest.fromTaxonomy(
                7L, name, "1.0.0", categoryId, topicId);
        ReflectionTestUtils.setField(interest, "id", id);
        return interest;
    }
}
