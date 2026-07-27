package com.bambi.service.card;

import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 카드 조회 서비스 (읽기 전용).
 * 소유자 범위는 userId 로 강제하고, 대외 식별자 publicId(UUID) 로만 단건을 찾는다.
 * (피드 목록 조회는 FeedService 가 담당 — 여기선 상세 단건만)
 */
@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * 카드 단건 상세. 프론트 카드 상세 화면의 새로고침/직접 진입용.
     * publicId 문자열이 UUID 형식이 아니거나(=존재할 수 없는 id) 내 카드가 아니면 NOT_FOUND.
     */
    @Transactional(readOnly = true)
    public CardResponse get(Long userId, String publicId) {
        UUID uuid = parseOrNotFound(publicId);
        Card card = cardRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(uuid, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        return CardResponse.from(card);
    }

    /**
     * 카드 공개설정 변경 (SNS/Week2) — 소유자만 자기 카드의 PUBLIC/PRIVATE 를 바꾼다.
     * 남의 카드는 존재 노출 없이 404. 변경 후 최신 카드 상태를 돌려준다.
     */
    @Transactional
    public CardResponse changeVisibility(Long userId, String publicId, String visibility) {
        UUID uuid = parseOrNotFound(publicId);
        Card card = cardRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(uuid, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        card.changeVisibility(visibility);   // dirty checking 으로 flush
        return CardResponse.from(card);
    }

    /** 잘못된 UUID 형식은 500 대신 404 로 다룬다(존재할 수 없는 카드). */
    private UUID parseOrNotFound(String publicId) {
        try {
            return UUID.fromString(publicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다.");
        }
    }
}
