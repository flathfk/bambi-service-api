package com.bambi.service.interest;

import com.bambi.service.agent.AgentContextSyncService;
import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.interest.dto.InterestRequest;
import com.bambi.service.interest.dto.InterestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관심사 CRUD (P0). 모든 요청 인증 필수, 소유자 범위는 @AuthenticationPrincipal 로 강제.
 */
@RestController
@RequestMapping("/api/interests")
public class InterestController {

    private final InterestService interestService;
    private final AgentContextSyncService contextSyncService;

    public InterestController(
            InterestService interestService,
            AgentContextSyncService contextSyncService) {
        this.interestService = interestService;
        this.contextSyncService = contextSyncService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InterestResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                @Valid @RequestBody InterestRequest request) {
        return ApiResponse.ok(interestService.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<InterestResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(interestService.list(principal.id()));
    }

    /** 온보딩 관심사 저장이 끝난 뒤 확정된 USER 관심사를 Agent 컨텍스트에 한 번 동기화한다. */
    @PostMapping("/sync")
    public ApiResponse<Void> sync(@AuthenticationPrincipal AuthPrincipal principal) {
        contextSyncService.syncUserContext(principal.id());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<InterestResponse> rename(@AuthenticationPrincipal AuthPrincipal principal,
                                                @PathVariable Long id,
                                                @Valid @RequestBody InterestRequest request) {
        return ApiResponse.ok(interestService.rename(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable Long id) {
        interestService.delete(principal.id(), id);
        return ApiResponse.ok();
    }
}
