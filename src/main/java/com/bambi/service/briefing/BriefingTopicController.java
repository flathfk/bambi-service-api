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
 * <p>⚠️ <b>2026-08-11 이후 이 API 에 저장한 값은 아침 브리핑 생성에 반영되지 않는다.</b>
 * 같은 날 "아침 주제는 사용자가 고르지 않는다"로 확정되면서 주제 결정이
 * <b>Agent의 개인 LLM Wiki 선정만 사용</b>하도록 바뀌었고
 * ({@link BriefingTopicService#resolveForMorningBriefing(Long, java.time.LocalDate)}),
 * 프론트 선택 화면도 제거됐다(service-web #74).
 * <ul>
 *   <li>{@link #get}·{@link #replace} 는 정상 동작하고 {@code user_briefing_topics} 에 저장도 된다 —
 *       <b>저장될 뿐 아무도 읽지 않는다.</b>
 *   <li>코드프리즈 중이라 지우지 않고 남겼다. 되살릴 가능성이 있는 쪽이라 급히 없앨 이유가 없었다.
 *       <b>정리 여부는 제출 후 결정</b>(agent-api {@code docs/agent-contract.md} 아침 브리핑 폴백 항목).
 *   <li>되살리려면 {@code BriefingTopicService.resolveForMorningBriefing} 의 주제 원천에
 *       이 저장값을 명시적으로 추가해야 한다. 컨트롤러만 살아 있어서는 동작하지 않는다.
 * </ul>
 * 계약 문서에만 적어두면 그 문서를 찾은 사람에게만 전달되므로 여기에도 남긴다.
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
