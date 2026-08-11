package com.bambi.service.generation;

import com.bambi.service.generation.dto.GenerationRequest;
import org.springframework.stereotype.Service;

/**
 * Agent 생성 접수와 Service 펜딩 등록을 하나의 순서로 묶는 공통 오케스트레이터.
 *
 * <p>Agent가 요청을 접수한 뒤에만 펜딩을 기록하므로 실패한 요청이 유령 슬롯을 만들지 않는다.
 * 펜딩 저장 실패는 {@link GenerationPendingService}가 격리하며, 호출자는 Agent 접수 결과를
 * 그대로 사용자에게 돌려줄 수 있다.
 */
@Service
public class GenerationSubmissionService {

    private final GenerationClient generationClient;
    private final GenerationPendingService pendingService;

    public GenerationSubmissionService(
            GenerationClient generationClient,
            GenerationPendingService pendingService) {
        this.generationClient = generationClient;
        this.pendingService = pendingService;
    }

    /** 생성 요청을 Agent에 접수한 뒤 동일 멱등키로 펜딩 상태를 등록한다. */
    public Submission submit(
            long userId,
            GenerationRequest request,
            String reportType,
            String displayTopic) {
        String agentJobId = generationClient.requestGeneration(userId, request);
        String pendingId = pendingService.register(
                userId,
                request.idempotencyKey(),
                reportType,
                displayTopic,
                request.contentType(),
                agentJobId);
        return new Submission(pendingId, agentJobId);
    }

    /** Service 펜딩 식별자와 Agent Job 식별자를 함께 전달하는 접수 결과. */
    public record Submission(String pendingId, String agentJobId) {
    }
}
