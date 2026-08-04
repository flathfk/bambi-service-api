package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentContextRequest;
import com.bambi.service.agent.outbox.AgentContextOutboxStore;
import com.bambi.service.onboarding.UserOnboardingSelection;
import com.bambi.service.onboarding.UserOnboardingSelectionRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AgentContextSyncService} — 컨텍스트 버전 증가와 Outbox 원자 적재 규약을 검증한다.
 */
class AgentContextSyncServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserOnboardingSelectionRepository onboardingSelectionRepository =
            mock(UserOnboardingSelectionRepository.class);
    private final AgentContextOutboxStore outboxStore = mock(AgentContextOutboxStore.class);
    private final AgentContextSyncService service =
            new AgentContextSyncService(userRepository, onboardingSelectionRepository, outboxStore);

    private static User newUser() {
        return new User("qa@bambi.test", "hash", "큐에이");
    }

    @Test
    void 첫_동기화는_버전1_payload를_outbox에_적재하고_사용자를_저장한다() {
        User user = newUser();   // 신규 → agent_context_version = 0
        when(userRepository.findByIdForAgentContextSync(1L)).thenReturn(Optional.of(user));
        when(onboardingSelectionRepository.findById(1L)).thenReturn(Optional.empty());

        service.enqueueUserContext(1L);

        ArgumentCaptor<AgentContextRequest> captor = ArgumentCaptor.forClass(AgentContextRequest.class);
        verify(outboxStore).enqueue(eq(1L), captor.capture());
        assertThat(captor.getValue().contextVersion()).isEqualTo(1);   // 0 → 1
        assertThat(captor.getValue().plan()).isEqualTo("free");
        assertThat(captor.getValue().selectedCategoryIds()).isEmpty();
        assertThat(captor.getValue().selectedTopicIds()).isEmpty();
        assertThat(user.getAgentContextVersion()).isEqualTo(1);        // 저장 대상에 반영
        verify(userRepository).save(user);
    }

    @Test
    void 재동기화는_버전을_단조_증가시킨다() {
        User user = newUser();
        user.bumpAgentContextVersion();   // 이미 한 번 동기화된 상태(=1)
        when(userRepository.findByIdForAgentContextSync(1L)).thenReturn(Optional.of(user));
        when(onboardingSelectionRepository.findById(1L)).thenReturn(Optional.empty());

        service.enqueueUserContext(1L);

        ArgumentCaptor<AgentContextRequest> captor = ArgumentCaptor.forClass(AgentContextRequest.class);
        verify(outboxStore).enqueue(eq(1L), captor.capture());
        assertThat(captor.getValue().contextVersion()).isEqualTo(2);   // 1 → 2 (STALE 방지)
    }

    @Test
    void 온보딩_선택이_있으면_분류체계와_category_topic을_payload에_담는다() {
        User user = newUser();
        UserOnboardingSelection selection = new UserOnboardingSelection(
                1L, "1.0.0", java.util.List.of("tech", "business"),
                java.util.List.of("ai_ml", "startup"));
        when(userRepository.findByIdForAgentContextSync(1L)).thenReturn(Optional.of(user));
        when(onboardingSelectionRepository.findById(1L)).thenReturn(Optional.of(selection));

        service.enqueueUserContext(1L);

        ArgumentCaptor<AgentContextRequest> captor = ArgumentCaptor.forClass(AgentContextRequest.class);
        verify(outboxStore).enqueue(eq(1L), captor.capture());
        assertThat(captor.getValue().interestTaxonomyVersion()).isEqualTo("1.0.0");
        assertThat(captor.getValue().selectedCategoryIds()).containsExactly("tech", "business");
        assertThat(captor.getValue().selectedTopicIds()).containsExactly("ai_ml", "startup");
    }

    @Test
    void 대상_사용자가_없으면_예외를_던지고_outbox를_적재하지_않는다() {
        when(userRepository.findByIdForAgentContextSync(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enqueueUserContext(99L))
                .isInstanceOf(IllegalStateException.class);

        verify(outboxStore, never()).enqueue(eq(99L), any());
    }
}
