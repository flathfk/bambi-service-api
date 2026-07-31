package com.bambi.service.feed;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.feed.dto.PublicCardResponse;
import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.follow.FollowRepository;
import com.bambi.service.like.LikeRepository;
import com.bambi.service.report.Report;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FeedService} 공개 피드 검증 — 특히 비로그인(게스트) 열람 시나리오.
 * 팀 정책(홈=공개 SNS): 게스트도 공개 피드/프로필을 볼 수 있고, 이때 liked 는 전부 false,
 * 팔로잉 스코프는 로그인 전제라 401 로 막는다.
 */
class FeedServiceTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final LikeRepository likeRepository = mock(LikeRepository.class);
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final com.bambi.service.report.ReportRepository reportRepository =
            mock(com.bambi.service.report.ReportRepository.class);
    private final FeedService service =
            new FeedService(cardRepository, likeRepository, followRepository, userRepository, reportRepository);

    @Test
    void 게스트도_공개피드를_볼_수_있고_liked_는_전부_false() {
        Card card = mock(Card.class);
        when(card.getId()).thenReturn(10L);
        when(card.getUserId()).thenReturn(2L);
        when(card.getPublicId()).thenReturn(UUID.randomUUID());
        when(card.getSources()).thenReturn(List.of());
        when(cardRepository.findPublicFeed(any())).thenReturn(List.of(card));
        when(likeRepository.countByCardIds(anyCollection())).thenReturn(List.of());
        User author = mock(User.class);
        when(author.getId()).thenReturn(2L);
        when(userRepository.findAllById(any())).thenReturn(List.of(author));

        // viewerId = null (비로그인)
        List<PublicCardResponse> feed = service.publicFeed(null, false, 20);

        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).liked()).isFalse();
        // 게스트는 "내 좋아요" 조회 쿼리를 아예 날리지 않아야 한다
        verify(likeRepository, never()).findLikedCardIds(any(), anyCollection());
    }

    @Test
    void myFeed_리포트없는_카드와_있는_카드를_섞어_NPE없이_반환한다() {
        // 리포트 없는(즉시) 카드 — reportId=null (불변맵 get(null) NPE 회귀 방지)
        Card noReport = mock(Card.class);
        when(noReport.getReportId()).thenReturn(null);
        when(noReport.getPublicId()).thenReturn(UUID.randomUUID());
        when(noReport.getSources()).thenReturn(List.of());
        // 리포트 있는 카드 — reportId 채워짐
        Card withReport = mock(Card.class);
        when(withReport.getReportId()).thenReturn(100L);
        when(withReport.getPublicId()).thenReturn(UUID.randomUUID());
        when(withReport.getSources()).thenReturn(List.of());
        when(cardRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(noReport, withReport));
        Report report = mock(Report.class);
        when(report.getId()).thenReturn(100L);
        UUID reportPublicId = UUID.randomUUID();
        when(report.getPublicId()).thenReturn(reportPublicId);
        when(reportRepository.findAllById(any())).thenReturn(List.of(report));

        List<CardResponse> feed = service.myFeed(1L);

        assertThat(feed).hasSize(2);
        assertThat(feed.get(0).reportId()).isNull();                 // 즉시 카드
        assertThat(feed.get(1).reportId()).isEqualTo(reportPublicId); // 리포트 연결 카드
    }

    @Test
    void 게스트가_팔로잉_스코프를_요청하면_401() {
        ApiException ex = catchThrowableOfType(
                () -> service.publicFeed(null, true, 20), ApiException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
        // 팔로잉 대상 조회조차 없어야 한다
        verify(followRepository, never()).findFolloweeIds(anyLong());
    }
}
