package com.bambi.service.auth;

import com.bambi.service.auth.dto.ChangePasswordRequest;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.RoleRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthService} 비밀번호 변경 검증 — 현재 비밀번호 불일치(401) / 성공(해시 교체) / 삭제 사용자(401).
 */
class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AuthService service = new AuthService(
            userRepository, roleRepository, passwordEncoder, tokenProvider, eventPublisher);

    @Test
    void 현재_비밀번호가_틀리면_401이고_교체하지_않는다() {
        User user = mock(User.class);
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getPasswordHash()).thenReturn("HASH");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        ApiException ex = catchThrowableOfType(
                () -> service.changePassword(1L, new ChangePasswordRequest("wrong", "newpassword1")),
                ApiException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        verify(user, never()).changePassword(any());   // 교체 시도조차 없어야
    }

    @Test
    void 현재_비밀번호가_맞으면_새_해시로_교체한다() {
        User user = mock(User.class);
        when(user.getDeletedAt()).thenReturn(null);
        when(user.getPasswordHash()).thenReturn("HASH");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current123", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("newpassword1")).thenReturn("NEWHASH");

        service.changePassword(1L, new ChangePasswordRequest("current123", "newpassword1"));

        verify(user).changePassword("NEWHASH");   // 평문이 아니라 인코딩된 해시로 저장
    }

    @Test
    void 삭제된_사용자는_토큰_유효해도_401_TOKEN() {
        User deleted = mock(User.class);
        when(deleted.getDeletedAt()).thenReturn(OffsetDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(deleted));

        ApiException ex = catchThrowableOfType(
                () -> service.changePassword(1L, new ChangePasswordRequest("current123", "newpassword1")),
                ApiException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    }
}
