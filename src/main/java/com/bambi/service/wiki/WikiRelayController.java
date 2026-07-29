package com.bambi.service.wiki;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.wiki.dto.WikiDocumentsResponse;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import com.bambi.service.wiki.dto.WikiTopNodesResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인 Wiki 조회 중계 (관심사 · LLM Wiki 화면). 전부 인증 필수, 사용자 범위는 인증 주체로 강제한다.
 *
 * <p>agent 자동 추출 관심은 {@code /tags}(topic→tag 리네임), 저장 자료는 {@code /documents}(schema 제외),
 * 연결 상위 노드는 {@code /graph/top-nodes}. 사용자가 직접 만드는 관심사는 여기가 아니라 {@code /api/interests} 다.
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiRelayController {

    private final WikiRelayService wikiRelayService;

    public WikiRelayController(WikiRelayService wikiRelayService) {
        this.wikiRelayService = wikiRelayService;
    }

    @GetMapping("/tags")
    public ApiResponse<WikiTagsResponse> tags(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(wikiRelayService.tags(principal.id()));
    }

    @GetMapping("/documents")
    public ApiResponse<WikiDocumentsResponse> documents(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(wikiRelayService.documents(principal.id()));
    }

    @GetMapping("/graph/top-nodes")
    public ApiResponse<WikiTopNodesResponse> topNodes(@AuthenticationPrincipal AuthPrincipal principal,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(wikiRelayService.topNodes(principal.id(), limit));
    }
}
