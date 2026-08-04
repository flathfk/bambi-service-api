package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminUserResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AdminUserService#setActive} — 활성/비활성 토글이 soft delete 표시와 status 파생에 반영되는지 검증.
 */
class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminUserService service = new AdminUserService(userRepository);

    /** createdAt(@CreationTimestamp)은 영속 시 채워지므로 단위 테스트에선 직접 심는다. */
    private User activeUser() {
        User user = new User("a@bambi.test", "hash", "홍길동");
        ReflectionTestUtils.setField(user, "createdAt", OffsetDateTime.now());
        return user;
    }

    @Test
    @DisplayName("비활성화: deletedAt 을 찍고 status 를 INACTIVE 로 내려준다")
    void deactivateMarksInactive() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AdminUserResponse response = service.setActive(1L, false);

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("활성화: deletedAt 을 지우고 status 를 ACTIVE 로 되돌린다")
    void activateClearsDeletedAt() {
        User user = activeUser();
        user.deactivate();   // 먼저 비활성 상태로
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AdminUserResponse response = service.setActive(1L, true);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("없는 사용자면 NOT_FOUND")
    void unknownUserThrowsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(9L, false))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
