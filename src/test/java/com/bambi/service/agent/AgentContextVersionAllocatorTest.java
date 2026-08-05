package com.bambi.service.agent;

import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 별도 트랜잭션 버전 할당기의 단조 증가 동작을 검증한다. */
class AgentContextVersionAllocatorTest {

    @Test
    void 사용자_context_version을_하나_증가시킨다() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("qa@bambi.test", "hash", "큐에이");
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        int allocated = new AgentContextVersionAllocator(repository).allocate(1L);

        assertThat(allocated).isEqualTo(1);
        assertThat(user.getAgentContextVersion()).isEqualTo(1);
    }
}
