package com.bambi.service.onboarding;

import com.bambi.service.agent.AgentContextSyncService;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.generation.GenerationPendingService;
import com.bambi.service.generation.GenerationSubmissionService;
import com.bambi.service.generation.dto.GenerationRequest;
import com.bambi.service.interest.Interest;
import com.bambi.service.interest.InterestRepository;
import com.bambi.service.interest.InterestSource;
import com.bambi.service.onboarding.dto.OnboardingCompleteRequest;
import com.bambi.service.onboarding.dto.OnboardingCompleteResponse;
import com.bambi.service.onboarding.dto.OnboardingReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 온보딩 완료 시 컨텍스트 동기화와 첫 리포트 생성을 Service 소유로 처리한다. */
@Service
public class OnboardingCompletionService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingCompletionService.class);

    private final InterestRepository interestRepository;
    private final AgentContextSyncService contextSyncService;
    private final OnboardingReportSelector reportSelector;
    private final GenerationSubmissionService submissionService;
    private final String contentType;

    public OnboardingCompletionService(
            InterestRepository interestRepository,
            AgentContextSyncService contextSyncService,
            OnboardingReportSelector reportSelector,
            GenerationSubmissionService submissionService,
            @Value("${app.scheduler.generation.content-type:interest_news_card}") String contentType) {
        this.interestRepository = interestRepository;
        this.contextSyncService = contextSyncService;
        this.reportSelector = reportSelector;
        this.submissionService = submissionService;
        this.contentType = contentType;
    }

    /** 선택 순서를 검증한 뒤 Agent 컨텍스트와 최대 3개의 ONBOARDING 생성 Job을 접수한다. */
    public OnboardingCompleteResponse complete(long userId, OnboardingCompleteRequest request) {
        List<Interest> activeInterests = interestRepository
                .findByUserIdAndSourceAndDeletedAtIsNull(userId, InterestSource.USER);
        List<Interest> orderedInterests = restoreSelectionOrder(
                request.orderedInterestIds(), activeInterests);

        // 모든 일반 관심사 변경 동기화도 소유권 플래그를 보내지만, 완료 요청에서는 사용자가
        // 선택한 정확한 순서로 다시 동기화해 선정·Agent 컨텍스트가 같은 원본을 보게 한다.
        contextSyncService.syncUserContext(userId, orderedInterests);

        List<OnboardingReportResponse> reports = new ArrayList<>();
        RuntimeException firstFailure = null;
        List<Interest> selected = reportSelector.select(orderedInterests);
        for (int index = 0; index < selected.size(); index++) {
            int slot = index + 1;
            Interest interest = selected.get(index);
            GenerationRequest generationRequest = GenerationRequest.singleTopic(
                    idempotencyKey(userId, slot),
                    interest.getName(),
                    contentType,
                    GenerationPendingService.REPORT_TYPE_ONBOARDING);
            try {
                GenerationSubmissionService.Submission submission = submissionService.submit(
                        userId,
                        generationRequest,
                        GenerationPendingService.REPORT_TYPE_ONBOARDING,
                        interest.getName());
                reports.add(new OnboardingReportResponse(
                        slot,
                        interest.getName(),
                        submission.pendingId(),
                        submission.agentJobId()));
            } catch (RuntimeException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                }
                log.warn(
                        "[OnboardingCompletion] 리포트 접수 실패 userId={}, slot={}, topic={}",
                        userId,
                        slot,
                        interest.getName(),
                        failure);
            }
        }
        if (reports.isEmpty() && firstFailure != null) {
            throw firstFailure;
        }
        return new OnboardingCompleteResponse(reports);
    }

    /** 사용자당 슬롯이 고정되어 재시도·중복 완료 요청에도 온보딩 리포트는 최대 3개다. */
    static String idempotencyKey(long userId, int slot) {
        return "onboarding:" + userId + ":slot:" + slot;
    }

    private List<Interest> restoreSelectionOrder(
            List<Long> orderedIds,
            List<Interest> activeInterests) {
        if (orderedIds.size() != new LinkedHashSet<>(orderedIds).size()) {
            throw invalidSelectionOrder();
        }
        Map<Long, Interest> byId = new LinkedHashMap<>();
        for (Interest interest : activeInterests) {
            byId.put(interest.getId(), interest);
        }
        if (byId.size() != orderedIds.size() || !byId.keySet().containsAll(orderedIds)) {
            throw invalidSelectionOrder();
        }
        return orderedIds.stream().map(byId::get).toList();
    }

    private ApiException invalidSelectionOrder() {
        return new ApiException(
                ErrorCode.VALIDATION_ERROR,
                "현재 관심사 전체의 선택 순서를 정확히 보내야 합니다.");
    }
}
