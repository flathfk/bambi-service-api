package com.bambi.service.card;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.card.dto.CardResponse;
import com.bambi.service.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카드 단건 조회 (P0) — 카드 상세 화면 진입/새로고침용.
 * 대외 식별자는 publicId(UUID). 소유자 범위는 @AuthenticationPrincipal 로 강제한다.
 * 목록은 GET /api/feed(FeedController) 가 담당한다.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/{publicId}")
    public ApiResponse<CardResponse> get(@AuthenticationPrincipal AuthPrincipal principal,
                                         @PathVariable String publicId) {
        return ApiResponse.ok(cardService.get(principal.id(), publicId));
    }
}
