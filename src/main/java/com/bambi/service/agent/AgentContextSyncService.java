package com.bambi.service.agent;

import com.bambi.service.agent.dto.AgentContextRequest;
import com.bambi.service.agent.outbox.AgentContextOutboxStore;
import com.bambi.service.onboarding.UserOnboardingSelection;
import com.bambi.service.onboarding.UserOnboardingSelectionRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 컨텍스트 동기화 요청을 Transactional Outbox에 적재한다(계약 §4).
 *
 * <p>agent 는 {@code context_version} 이 사용자별 단조 증가일 것을 요구하고, 같거나 작은 값을
 * 재전송하면 STALE_CONTEXT_VERSION 으로 거부한다(§4.3). 그래서 적재할 때 service-db 의
 * {@code users.agent_context_version} 을 잠근 상태로 +1 하고, 같은 트랜잭션에 payload를 보존한다.
 * 가입뿐 아니라 이후 설정 변경(플랜·개인화·차단) 재동기도 이 진입점을 쓰면 된다.
 */
@Service
public class AgentContextSyncService {

    private final UserRepository userRepository;
    private final UserOnboardingSelectionRepository onboardingSelectionRepository;
    private final AgentContextOutboxStore outboxStore;

    public AgentContextSyncService(UserRepository userRepository,
                                   UserOnboardingSelectionRepository onboardingSelectionRepository,
                                   AgentContextOutboxStore outboxStore) {
        this.userRepository = userRepository;
        this.onboardingSelectionRepository = onboardingSelectionRepository;
        this.outboxStore = outboxStore;
    }

    /**
     * 사용자 컨텍스트 버전과 payload를 호출한 비즈니스 트랜잭션에 원자적으로 적재한다.
     * 실제 HTTP는 커밋 뒤 Dispatcher가 처리하므로 실패해도 Outbox 행이 재시도 근거로 남는다.
     */
    @Transactional
    public void enqueueUserContext(long userId) {
        User user = userRepository.findByIdForAgentContextSync(userId)
                .orElseThrow(() -> new IllegalStateException("컨텍스트 동기화 대상 사용자 없음: " + userId));
        int version = user.bumpAgentContextVersion();
        userRepository.save(user);
        AgentContextRequest request = onboardingSelectionRepository.findById(userId)
                .map(selection -> requestFor(version, selection))
                .orElseGet(() -> AgentContextRequest.forVersion(version));
        outboxStore.enqueue(userId, request);
    }

    /** 저장된 온보딩 선택을 지정 Context Version의 Agent 요청으로 변환한다. */
    private static AgentContextRequest requestFor(int version, UserOnboardingSelection selection) {
        return AgentContextRequest.forVersion(
                version,
                selection.getInterestTaxonomyVersion(),
                selection.getSelectedCategoryIds(),
                selection.getSelectedTopicIds());
    }
}
