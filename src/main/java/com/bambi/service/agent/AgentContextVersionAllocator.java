package com.bambi.service.agent;

import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Agent 컨텍스트 버전을 항상 별도 트랜잭션에서 단조 증가시킨다. */
@Service
public class AgentContextVersionAllocator {

    private final UserRepository userRepository;

    public AgentContextVersionAllocator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** AFTER_COMMIT 리스너에서도 증가 값이 실제 DB에 커밋되도록 새 트랜잭션을 사용한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int allocate(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("컨텍스트 동기화 대상 사용자 없음: " + userId));
        return user.bumpAgentContextVersion();
    }

    /**
     * agent 가 409 STALE 로 알려준 현재 버전에 맞춰 service 카운터를 점프시키고 다음 버전을 반환한다.
     * 두 카운터가 어긋났을 때 정합용 — 이 값으로 재전송하면 STALE 이 풀린다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int reconcile(long userId, int agentCurrentVersion) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("컨텍스트 동기화 대상 사용자 없음: " + userId));
        return user.reconcileAgentContextVersion(agentCurrentVersion);
    }
}
