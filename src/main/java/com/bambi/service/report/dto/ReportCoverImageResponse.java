package com.bambi.service.report.dto;

import com.bambi.service.report.Report;

/** 리포트 상단 대표 이미지와 실제 인용 출처 응답. */
public record ReportCoverImageResponse(
        String url,
        String sourceUrl,
        String sourceTitle,
        String reference) {

    /** 대표 이미지가 완전하게 저장된 리포트만 화면 계약으로 변환한다. */
    public static ReportCoverImageResponse from(Report report) {
        if (report == null
                || report.getCoverImageUrl() == null
                || report.getCoverImageSourceUrl() == null) {
            return null;
        }
        return new ReportCoverImageResponse(
                report.getCoverImageUrl(),
                report.getCoverImageSourceUrl(),
                report.getCoverImageSourceTitle(),
                report.getCoverImageReference());
    }
}
