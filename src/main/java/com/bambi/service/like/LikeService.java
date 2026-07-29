package com.bambi.service.like;

import com.bambi.service.card.Card;
import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.like.dto.LikeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 좋아요 도메인 (SNS/Week2). 공개(PUBLIC) 카드에만 좋아요할 수 있다.
 * 좋아요/취소는 멱등 — 낙관적 업데이트 재시도에 안전.
 * 남의 비공개 카드는 존재 노출 없이 404(공개피드에 없는 카드).
 */
@Service
public class LikeService {

    private static final String PUBLIC = "PUBLIC";

    private final LikeRepository likeRepository;
    private final CardRepository cardRepository;

    public LikeService(LikeRepository likeRepository, CardRepository cardRepository) {
        this.likeRepository = likeRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public LikeResponse like(Long userId, String cardPublicId) {
        Card card = resolveAliveCard(cardPublicId);
        // 좋아요는 공개(PUBLIC) 카드에만. 비공개/없음은 존재 노출 없이 404.
        if (!PUBLIC.equals(card.getVisibility())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        likeRepository.insertIgnore(userId, card.getId());   // 멱등
        return new LikeResponse(true, likeRepository.countByCardId(card.getId()));
    }

    @Transactional
    public LikeResponse unlike(Long userId, String cardPublicId) {
        // 취소는 PUBLIC 검사를 하지 않는다: 좋아요한 뒤 소유자가 비공개로 바꿔도 취소할 수 있어야 한다.
        // deleteRelation 은 없어도 0건이라 멱등하게 안전하다.
        Card card = resolveAliveCard(cardPublicId);
        likeRepository.deleteRelation(userId, card.getId());
        return new LikeResponse(false, likeRepository.countByCardId(card.getId()));
    }

    /** publicId 로 살아있는 카드 조회. 형식 오류/없음은 존재 노출 없이 404. (PUBLIC 여부는 호출부 판단) */
    private Card resolveAliveCard(String cardPublicId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cardPublicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        return cardRepository.findByPublicIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
    }
}
