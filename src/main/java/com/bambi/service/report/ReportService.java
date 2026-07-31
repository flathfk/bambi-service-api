package com.bambi.service.report;

import com.bambi.service.card.CardRepository;
import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.report.dto.ReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 리포트(본문) 조회 서비스 (읽기 전용).
 * 접근 권한은 "카드 visibility 를 따라간다": 리포트가 내 것이거나,
 * 이 리포트를 참조하는 PUBLIC 카드가 있으면 열람 가능. 그 외는 존재 노출 없이 404.
 */
@Service
public class ReportService {

    private static final String PUBLIC = "PUBLIC";

    private final ReportRepository reportRepository;
    private final CardRepository cardRepository;

    public ReportService(ReportRepository reportRepository, CardRepository cardRepository) {
        this.reportRepository = reportRepository;
        this.cardRepository = cardRepository;
    }

    /** 리포트 본문 상세. 카드 상세 화면의 본문 진입용. */
    @Transactional(readOnly = true)
    public ReportResponse get(Long viewerId, String publicId) {
        UUID uuid = parseOrNotFound(publicId);
        Report report = reportRepository.findByPublicIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다."));

        boolean mine = viewerId != null && report.getUserId().equals(viewerId);
        boolean publiclyLinked =
                cardRepository.existsByReportIdAndVisibilityAndDeletedAtIsNull(report.getId(), PUBLIC);
        if (!mine && !publiclyLinked) {
            // 비공개 남의 리포트 — 존재 노출 없이 404.
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다.");
        }
        return ReportResponse.from(report);
    }

    /** 잘못된 UUID 형식은 500 대신 404 로 다룬다(존재할 수 없는 리포트). */
    private UUID parseOrNotFound(String publicId) {
        try {
            return UUID.fromString(publicId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다.");
        }
    }
}
