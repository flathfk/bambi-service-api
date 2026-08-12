package com.bambi.service.user;

import com.bambi.service.auth.dto.UserSummary;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.dto.UpdateProfileRequest;
import com.bambi.service.user.dto.UpdateSettingsRequest;
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
    void 새_계정은_변경점_추적이_켜진_상태로_시작한다() {
        // V30(2026-08-12) — 기본값을 opt-in(false)에서 opt-out(true)으로 뒤집었다.
        // false 로 두면 가입 직후 첫 보고서에 델타가 안 붙고, 사용자가 /settings 를 찾아
        // 켜기 전까지 "지난번 이후 뭐가 달라졌나"라는 핵심 값을 한 번도 못 본다.
        // DB DEFAULT 와 엔티티 초기값이 어긋나면 가입 경로에 따라 값이 갈리므로 함께 고정한다.
        assertThat(new User("new@bambi.local", "hash", "새사람").isChangeHistoryEnabled()).isTrue();
    }

    @Test
    void 설정에서_변경점_추적을_끌_수_있다() {
        User user = liveUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.updateSettings(1L, new UpdateSettingsRequest(null, null, false));

        assertThat(user.isChangeHistoryEnabled()).isFalse();
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

    // ── 사용자 설정(V17) ─────────────────────────────────────────

    @Test
    void 설정_공개범위만_바꾸면_알림수신은_유지된다() {
        User user = liveUser();   // 기본값: PRIVATE / 알림 true
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary res = service.updateSettings(1L, new UpdateSettingsRequest("PUBLIC", null, null));

        assertThat(res.defaultCardVisibility()).isEqualTo("PUBLIC");
        assertThat(res.reportReadyNotification()).isTrue();   // null 미전송 → 미변경
    }

    @Test
    void 설정_알림만_끄면_공개범위는_유지된다() {
        User user = liveUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary res = service.updateSettings(1L, new UpdateSettingsRequest(null, false, null));

        assertThat(res.defaultCardVisibility()).isEqualTo("PRIVATE");   // null 미전송 → 미변경
        assertThat(res.reportReadyNotification()).isFalse();
    }

    @Test
    void 잘못된_공개범위는_VALIDATION_ERROR_이고_아무것도_안바뀐다() {
        User user = liveUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = catchThrowableOfType(
                () -> service.updateSettings(1L, new UpdateSettingsRequest("BOGUS", false, null)),
                ApiException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(user.getDefaultCardVisibility()).isEqualTo("PRIVATE");   // 검증 실패 → 반영 안 됨
        assertThat(user.isReportReadyNotification()).isTrue();
    }

    @Test
    void 설정_델타추적만_켜면_나머지는_유지된다() {
        User user = liveUser();   // 기본값: PRIVATE / 알림 true / 델타 false
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummary res = service.updateSettings(1L, new UpdateSettingsRequest(null, null, true));

        assertThat(res.changeHistoryEnabled()).isTrue();
        assertThat(res.defaultCardVisibility()).isEqualTo("PRIVATE");   // null 미전송 → 미변경
        assertThat(res.reportReadyNotification()).isTrue();
    }
}
