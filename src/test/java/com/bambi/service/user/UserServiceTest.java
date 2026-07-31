package com.bambi.service.user;

import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserService} 프로필 편집 검증 — 표시명/소개 반영, 핸들 정규화·형식·중복.
 */
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService service = new UserService(userRepository);

    private User liveUser() {
        User user = new User("u@bambi.local", "hash", "이전이름");
        return user;
    }

    @Test
    void 표시명과_소개를_반영하고_핸들은_소문자로_정규화한다() {
        User user = liveUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot(anyString(), anyLong())).thenReturn(false);

        UserSummary result = service.updateProfile(1L,
                new UpdateProfileRequest("  Parami ", "  ParaMi_01 ", "  매일 아침 브리핑  "));

        assertThat(result.displayName()).isEqualTo("Parami");
        assertThat(result.username()).isEqualTo("parami_01");
        assertThat(result.bio()).isEqualTo("매일 아침 브리핑");
        assertThat(result.roles()).isEqualTo(Set.of());
    }

    @Test
    void 빈_소개는_지움으로_저장되고_핸들_미전송은_변경_없음() {
        User user = liveUser();
        user.changeUsername("keepme");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary result = service.updateProfile(1L, new UpdateProfileRequest("이름", null, "   "));

        assertThat(result.bio()).isNull();
        assertThat(result.username()).isEqualTo("keepme");
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }

    @Test
    void 형식이_틀린_핸들은_VALIDATION_ERROR() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(liveUser()));

        ApiException ex = catchThrowableOfType(
                () -> service.updateProfile(1L, new UpdateProfileRequest("이름", "한글핸들!", null)),
                ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }

    @Test
    void 이미_쓰는_핸들이면_DUPLICATE_RESOURCE() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(liveUser()));
        when(userRepository.existsByUsernameAndIdNot("taken", 1L)).thenReturn(true);

        ApiException ex = catchThrowableOfType(
                () -> service.updateProfile(1L, new UpdateProfileRequest("이름", "taken", null)),
                ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void 같은_핸들_재전송은_중복검사_없이_통과한다() {
        User user = liveUser();
        user.changeUsername("parami");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary result = service.updateProfile(1L, new UpdateProfileRequest("이름", "parami", null));

        assertThat(result.username()).isEqualTo("parami");
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }
}
