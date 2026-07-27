package com.bambi.service.feed;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.card.dto.CardResponse;
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
     * @param viewerId 조회자 — "내 좋아요 여부"와 팔로잉 스코프 기준.
     */
    @Transactional(readOnly = true)
    public List<PublicCardResponse> publicFeed(Long viewerId, boolean followingOnly, int limit) {
        Pageable page = PageRequest.of(0, clampLimit(limit));

        List<Card> cards;
        if (followingOnly) {
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
        // ② 내가 좋아요한 카드 집합 (1 IN 쿼리)
        Set<Long> likedIds = new HashSet<>(likeRepository.findLikedCardIds(viewerId, cardIds));
        // ③ 작성자 정보 (1 IN 쿼리)
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
