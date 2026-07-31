package com.bambi.service.follow;

import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.follow.dto.FollowResponse;
import com.bambi.service.follow.dto.ProfileResponse;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 팔로우 도메인 (SNS/Week2). 대상 사용자는 대외 식별자 publicId(UUID)로만 가리킨다.
 * 팔로우/언팔은 멱등(ON CONFLICT DO NOTHING / 없어도 무시) — 낙관적 업데이트 재시도에 안전.
 * 자기 자신 팔로우는 서비스에서 400 으로 막고, DB CHECK(V4)가 이중 방어한다.
 */
@Service
public class FollowService {

    private static final String PUBLIC = "PUBLIC";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         CardRepository cardRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public FollowResponse follow(Long followerId, String followeePublicId) {
        Long followeeId = resolveUserId(followeePublicId);
        if (followeeId.equals(followerId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "자기 자신은 팔로우할 수 없습니다.");
        }
        followRepository.insertIgnore(followerId, followeeId);   // 멱등
        return new FollowResponse(true, followRepository.countByFolloweeId(followeeId));
    }

    @Transactional
    public FollowResponse unfollow(Long followerId, String followeePublicId) {
        Long followeeId = resolveUserId(followeePublicId);
        followRepository.deleteRelation(followerId, followeeId);  // 없어도 멱등
        return new FollowResponse(false, followRepository.countByFolloweeId(followeeId));
    }

    /**
     * 공개 프로필 + 팔로우 통계. following = 조회자(viewerId)가 이 사용자를 팔로우 중인지.
     * viewerId=null(비로그인 게스트)이면 following 은 항상 false(조회 없이).
     */
    @Transactional(readOnly = true)
    public ProfileResponse profile(Long viewerId, String targetPublicId) {
        User target = resolveUser(targetPublicId);
        Long targetId = target.getId();
        boolean following = viewerId != null
                && !targetId.equals(viewerId)
                && followRepository.existsByFollowerIdAndFolloweeId(viewerId, targetId);
        return new ProfileResponse(
                target.getPublicId(),
                target.getUsername(),
                target.getDisplayName(),
                target.getBio(),
                target.getCreatedAt(),
                followRepository.countByFolloweeId(targetId),
                followRepository.countByFollowerId(targetId),
                following,
                cardRepository.countByUserIdAndVisibilityAndDeletedAtIsNull(targetId, PUBLIC));
    }

    private Long resolveUserId(String publicId) {
        return resolveUser(publicId).getId();
    }

    /** publicId 로 살아있는 사용자 조회. 형식 오류/없음은 존재 노출 없이 404. */
    private User resolveUser(String publicId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(publicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return userRepository.findByPublicIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
