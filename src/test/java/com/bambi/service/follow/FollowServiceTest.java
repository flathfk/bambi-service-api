package com.bambi.service.follow;

import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.follow.dto.FollowResponse;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FollowService} 검증 — 자기팔로우 차단 / 멱등 팔로우 / 언팔.
 */
class FollowServiceTest {

    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final FollowService service = new FollowService(followRepository, userRepository, cardRepository);

    @Test
    void 자기_자신은_팔로우할_수_없다() {
        User me = mock(User.class);
        when(me.getId()).thenReturn(1L);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(me));

        ApiException ex = catchThrowableOfType(
                () -> service.follow(1L, UUID.randomUUID().toString()), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(followRepository, never()).insertIgnore(anyLong(), anyLong());   // 저장 시도조차 없어야
    }

    @Test
    void 없는_사용자를_팔로우하면_NOT_FOUND() {
        when(userRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> service.follow(1L, UUID.randomUUID().toString()), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 팔로우는_멱등이고_팔로워수를_돌려준다() {
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(target));
        when(followRepository.countByFolloweeId(2L)).thenReturn(5L);

        FollowResponse res = service.follow(1L, UUID.randomUUID().toString());

        assertThat(res.following()).isTrue();
        assertThat(res.followerCount()).isEqualTo(5L);
        verify(followRepository).insertIgnore(1L, 2L);   // ON CONFLICT DO NOTHING (중복이어도 예외 없음)
    }

    @Test
    void 언팔은_없어도_멱등이다() {
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(target));
        when(followRepository.countByFolloweeId(2L)).thenReturn(0L);

        FollowResponse res = service.unfollow(1L, UUID.randomUUID().toString());

        assertThat(res.following()).isFalse();
        assertThat(res.followerCount()).isEqualTo(0L);
        verify(followRepository).deleteRelation(1L, 2L);
    }

    @Test
    void 프로필에는_bio_와_가입시점이_들어간다() {
        java.time.OffsetDateTime joined = java.time.OffsetDateTime.parse("2026-03-01T00:00:00+09:00");
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(target.getBio()).thenReturn("매일 아침 브리핑");
        when(target.getCreatedAt()).thenReturn(joined);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(target));

        var profile = service.profile(null, UUID.randomUUID().toString());   // 게스트 열람

        assertThat(profile.bio()).isEqualTo("매일 아침 브리핑");
        assertThat(profile.joinedAt()).isEqualTo(joined);
        assertThat(profile.following()).isFalse();   // 게스트는 팔로우 여부 조회 없이 false
    }
}
