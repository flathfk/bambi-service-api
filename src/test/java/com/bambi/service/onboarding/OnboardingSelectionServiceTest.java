package com.bambi.service.onboarding;

import com.bambi.service.onboarding.dto.OnboardingSelectionRequest;
import com.bambi.service.onboarding.dto.OnboardingSelectionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link OnboardingSelectionService}의 전체 교체와 동기화 이벤트 발행을 검증한다. */
class OnboardingSelectionServiceTest {

    private final UserOnboardingSelectionRepository repository =
            mock(UserOnboardingSelectionRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final OnboardingSelectionService service =
            new OnboardingSelectionService(repository, eventPublisher, "1.0.0");

    @Test
    void 선택을_정규화해_저장하고_context_재동기화_이벤트를_발행한다() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        OnboardingSelectionRequest request = new OnboardingSelectionRequest(
                List.of("tech", " tech ", "business"),
                List.of("ai_ml", "startup", "ai_ml"));

        OnboardingSelectionResponse response = service.replace(7L, request);

        ArgumentCaptor<UserOnboardingSelection> captor =
                ArgumentCaptor.forClass(UserOnboardingSelection.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getInterestTaxonomyVersion()).isEqualTo("1.0.0");
        assertThat(response.selectedCategoryIds()).containsExactly("tech", "business");
        assertThat(response.selectedTopicIds()).containsExactly("ai_ml", "startup");
        verify(eventPublisher).publishEvent(eq(new OnboardingSelectionsChangedEvent(7L)));
    }

    @Test
    void 선택이_없으면_현재_taxonomy_version의_빈_목록을_반환한다() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        OnboardingSelectionResponse response = service.get(7L);

        assertThat(response.interestTaxonomyVersion()).isEqualTo("1.0.0");
        assertThat(response.selectedCategoryIds()).isEmpty();
        assertThat(response.selectedTopicIds()).isEmpty();
    }

    @Test
    void 빈_taxonomy_version_설정은_기동_전에_거절한다() {
        assertThatThrownBy(() -> new OnboardingSelectionService(repository, eventPublisher, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
