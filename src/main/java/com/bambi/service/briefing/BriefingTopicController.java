package com.bambi.service.briefing;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.briefing.dto.BriefingTopicsRequest;
import com.bambi.service.briefing.dto.BriefingTopicsResponse;
import com.bambi.service.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아침 브리핑 주제 선택 (설정 화면 · bambi-service-web #59 여진).
 *
 * <p>우석님이 맡은 {@code PATCH /api/users/me/settings}(기본 공개범위·알림)와 분리했다 —
 * 배열이라 부분 갱신 의미가 애매하고, 담당·배포도 나뉜다.
 *
 * <p>저장은 <b>전체 교체(PUT)</b>다. 빈 배열은 "선택 해제"로 정상 처리한다.
 */
@RestController
@RequestMapping("/api/users/me/briefing-topics")
public class BriefingTopicController {

    private final BriefingTopicService service;

    public BriefingTopicController(BriefingTopicService service) {
        this.service = service;
    }

    /** 내 선택값 — 미선택이면 빈 배열(404 아님). */
    @GetMapping
    public ApiResponse<BriefingTopicsResponse> get(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(BriefingTopicsResponse.of(service.get(principal.id())));
    }

    /** 선택값 전체 교체. 응답은 정규화된 결과라 화면은 이 값으로 상태를 갱신하면 된다. */
    @PutMapping
    public ApiResponse<BriefingTopicsResponse> replace(@AuthenticationPrincipal AuthPrincipal principal,
                                                       @Valid @RequestBody BriefingTopicsRequest request) {
        return ApiResponse.ok(BriefingTopicsResponse.of(service.replace(principal.id(), request.topics())));
    }
}
