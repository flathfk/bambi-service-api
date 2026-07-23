package com.bambi.service.admin;

import com.bambi.service.admin.dto.AdminAiLogDetailResponse;
import com.bambi.service.admin.dto.AdminAiLogResponse;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 AI 처리 로그 조회.
 *
 * 요청(ai_request_logs)을 최신순으로 훑으며 각 요청의 최신 응답을 붙여 한 줄로 만든다.
 * 요청 수가 많아지면 N+1 이 되지만, 로그 적재는 아직 시작 전(P1)이라 지금은 데이터가 없다.
 * 실제 트래픽이 붙어 성능이 문제되면 join 쿼리로 바꾼다 — 그때 벤치로 확인하고 옮긴다.
 */
@Service
public class AdminAiLogService {

    private final AiRequestLogRepository requestLogRepository;
    private final AiResponseLogRepository responseLogRepository;

    public AdminAiLogService(AiRequestLogRepository requestLogRepository,
                             AiResponseLogRepository responseLogRepository) {
        this.requestLogRepository = requestLogRepository;
        this.responseLogRepository = responseLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminAiLogResponse> listLogs() {
        return requestLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(request -> {
                    AiResponseLog response = responseLogRepository
                            .findFirstByRequestIdOrderByCreatedAtDesc(request.getId())
                            .orElse(null);
                    return AdminAiLogResponse.of(request, response);
                })
                .toList();
    }

    /**
     * AI 로그 한 건의 상세(요청·응답 본문 포함)를 조회한다.
     * 요청 로그가 없으면 NOT_FOUND. 응답이 아직 없으면 처리 중이라 본문은 요청만 채워진다.
     */
    @Transactional(readOnly = true)
    public AdminAiLogDetailResponse getLogDetail(long requestId) {
        AiRequestLog request = requestLogRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                        "AI 로그를 찾을 수 없습니다 (id=" + requestId + ")"));
        AiResponseLog response = responseLogRepository
                .findFirstByRequestIdOrderByCreatedAtDesc(requestId)
                .orElse(null);
        return AdminAiLogDetailResponse.of(request, response);
    }
}
