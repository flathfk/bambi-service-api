package com.bambi.service.scrap;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.scrap.dto.ScrapCardResponse;
import com.bambi.service.scrap.dto.ScrapResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 스크랩 (Week3 SNS). 인증 필수.
 * - POST/DELETE /api/cards/{publicId}/scrap : 담기/취소 (멱등, 공개 카드만)
 * - GET /api/scraps                          : 내 스크랩 목록 (아직 PUBLIC 인 것만)
 */
@RestController
public class ScrapController {

    private final ScrapService scrapService;

    public ScrapController(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    @PostMapping("/api/cards/{publicId}/scrap")
    public ApiResponse<ScrapResponse> scrap(@AuthenticationPrincipal AuthPrincipal principal,
                                            @PathVariable String publicId) {
        return ApiResponse.ok(scrapService.scrap(principal.id(), publicId));
    }

    @DeleteMapping("/api/cards/{publicId}/scrap")
    public ApiResponse<ScrapResponse> unscrap(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable String publicId) {
        return ApiResponse.ok(scrapService.unscrap(principal.id(), publicId));
    }

    @GetMapping("/api/scraps")
    public ApiResponse<List<ScrapCardResponse>> myScraps(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(scrapService.myScraps(principal.id()));
    }
}
