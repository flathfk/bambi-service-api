package com.bambi.service.agent;

import com.bambi.service.agent.outbox.AgentContextOutboxDispatcher;
import com.bambi.service.onboarding.OnboardingSelectionsChangedEvent;
import com.bambi.service.user.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 가입 트랜잭션에 사용자 컨텍스트 Outbox를 적재하고, 커밋 직후 즉시 전달을 시도한다.
 *
 * <p>BEFORE_COMMIT 적재로 사용자와 전송 의도를 원자적으로 보존한다. AFTER_COMMIT 전송은:
 * <ul>
 *   <li>agent 호출이 가입 트랜잭션을 붙잡지 않는다 (HTTP 대기 분리).
 *   <li>agent 가 죽거나 프로세스가 중단돼도 Outbox Worker가 같은 payload를 재시도한다.
 * </ul>
 */
@Component
public class AgentContextSyncListener {

    private static final Logger log = LoggerFactory.getLogger(AgentContextSyncListener.class);

    private final AgentContextSyncService contextSyncService;
    private final AgentContextOutboxDispatcher outboxDispatcher;

    public AgentContextSyncListener(AgentContextSyncService contextSyncService,
                                    AgentContextOutboxDispatcher outboxDispatcher) {
        this.contextSyncService = contextSyncService;
        this.outboxDispatcher = outboxDispatcher;
    }

    /** 회원가입과 같은 트랜잭션에 Outbox 행을 반드시 적재한다. 실패하면 가입도 롤백된다. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enqueueBeforeCommit(UserRegisteredEvent event) {
        contextSyncService.enqueueUserContext(event.userId());
    }

    /** 온보딩 선택과 같은 트랜잭션에 새 버전의 전체 Context Outbox를 적재한다. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enqueueOnboardingBeforeCommit(OnboardingSelectionsChangedEvent event) {
        contextSyncService.enqueueUserContext(event.userId());
    }

    /** 커밋 직후 지연 없이 보내되, 실패는 Outbox에 남겨 가입 응답을 되돌리지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(UserRegisteredEvent event) {
        dispatchOrDefer(event.userId(), "가입");
    }

    /** 온보딩 선택 커밋 직후 새 Snapshot을 즉시 보내고 실패하면 Worker에 맡긴다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchOnboardingAfterCommit(OnboardingSelectionsChangedEvent event) {
        dispatchOrDefer(event.userId(), "온보딩 선택");
    }

    /** 커밋된 Outbox를 즉시 보내되 실패가 비즈니스 요청 결과를 되돌리지 않게 한다. */
    private void dispatchOrDefer(long userId, String source) {
        try {
            outboxDispatcher.dispatchForUser(userId);
        } catch (Exception e) {
            log.warn("[ContextOutbox] {} 직후 전송 시도 실패 (userId={}) — 폴링 워커가 재시도",
                    source, userId, e);
        }
    }
}
