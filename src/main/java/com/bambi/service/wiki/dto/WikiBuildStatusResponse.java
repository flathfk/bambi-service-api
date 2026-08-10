package com.bambi.service.wiki.dto;

import java.time.OffsetDateTime;

/** 사용자 Wiki 빌드의 집계 상태. */
public record WikiBuildStatusResponse(
        String status,
        long activeCount,
        OffsetDateTime updatedAt,
        String errorCode) {
}
