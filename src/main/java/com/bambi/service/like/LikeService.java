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
        Long cardId = resolvePublicCardId(cardPublicId);
        likeRepository.insertIgnore(userId, cardId);   // 멱등
        return new LikeResponse(true, likeRepository.countByCardId(cardId));
    }

    @Transactional
    public LikeResponse unlike(Long userId, String cardPublicId) {
        Long cardId = resolvePublicCardId(cardPublicId);
        likeRepository.deleteRelation(userId, cardId);  // 없어도 멱등
        return new LikeResponse(false, likeRepository.countByCardId(cardId));
    }

    /** 공개 카드만 좋아요 대상. 형식 오류/없음/비공개는 모두 404 로 통일한다. */
    private Long resolvePublicCardId(String cardPublicId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(cardPublicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        Card card = cardRepository.findByPublicIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        if (!PUBLIC.equals(card.getVisibility())) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
        return card.getId();
    }
}
