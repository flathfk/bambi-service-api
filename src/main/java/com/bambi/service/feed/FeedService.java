package com.bambi.service.feed;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.feed.dto.PublicCardResponse;
import com.bambi.service.follow.FollowRepository;
import com.bambi.service.like.LikeRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 카드 피드.
 * - 내 피드(P0): 내 카드 전부 최신순.
 * - 공개 피드(SNS/Week2): PUBLIC 카드 최신순 + 작성자/좋아요 수/내 좋아요 여부.
 *   좋아요 수·내 좋아요·작성자는 카드별 재조회 없이 각각 1번의 배치 쿼리로 합친다(N+1 차단).
 */
@Service
public class FeedService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final CardRepository cardRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FeedService(CardRepository cardRepository,
                       LikeRepository likeRepository,
                       FollowRepository followRepository,
                       UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.likeRepository = likeRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> myFeed(Long userId) {
        return cardRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(CardResponse::from)
                .toList();
    }

    /**
     * 공개 피드. followingOnly=true 면 내가 팔로우하는 작성자의 공개 카드만.
     * @param viewerId 조회자 id. 비로그인(게스트)이면 null — liked 는 전부 false.
     *                 팔로잉 스코프는 로그인 전제라 게스트면 401 로 막는다.
     */
    @Transactional(readOnly = true)
    public List<PublicCardResponse> publicFeed(Long viewerId, boolean followingOnly, int limit) {
        Pageable page = PageRequest.of(0, clampLimit(limit));

        List<Card> cards;
        if (followingOnly) {
            if (viewerId == null) {
                throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN, "팔로잉 피드는 로그인이 필요합니다.");
            }
            List<Long> authorIds = followRepository.findFolloweeIds(viewerId);
            if (authorIds.isEmpty()) {
                return List.of();   // 아무도 팔로우 안 함 → 빈 피드(프론트 Empty State)
            }
            cards = cardRepository.findPublicFeedByAuthors(authorIds, page);
        } else {
            cards = cardRepository.findPublicFeed(page);
        }
        if (cards.isEmpty()) {
            return List.of();
        }

        List<Long> cardIds = cards.stream().map(Card::getId).toList();

        // ① 카드별 좋아요 수 (1 group-by 쿼리)
        Map<Long, Long> likeCounts = likeRepository.countByCardIds(cardIds).stream()
                .collect(Collectors.toMap(LikeRepository.CardLikeCount::getCardId,
                        LikeRepository.CardLikeCount::getCount));
        // ② 내가 좋아요한 카드 집합 (1 IN 쿼리) — 게스트(viewerId=null)는 조회 없이 전부 false
        Set<Long> likedIds = viewerId == null
                ? Set.of()
                : new HashSet<>(likeRepository.findLikedCardIds(viewerId, cardIds));
        // ③ 작성자 정보 (1 IN 쿼리)
        // TODO(follow-up 이슈): 탈퇴(soft delete) 작성자의 PUBLIC 카드가 피드에 남는다.
        //   FK CASCADE 는 하드 삭제에만 반응하므로, "작성자 생존(deleted_at IS NULL) 필터"를
        //   피드 쿼리/서비스에 추가해야 한다. 이번 PR 범위 밖 — 별도 이슈로 처리.
        List<Long> authorIds = cards.stream().map(Card::getUserId).distinct().toList();
        Map<Long, User> authors = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return cards.stream()
                .map(card -> PublicCardResponse.from(
                        card,
                        authors.get(card.getUserId()),
                        likeCounts.getOrDefault(card.getId(), 0L),
                        likedIds.contains(card.getId())))
                .toList();
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
