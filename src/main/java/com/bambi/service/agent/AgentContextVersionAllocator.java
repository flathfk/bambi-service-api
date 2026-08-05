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
}
