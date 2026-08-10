package com.bambi.service.generation.dto;

import com.bambi.service.generation.GenerationPending;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 생성 작업 1건 — 홈 [내 보고서] 진행·완료·실패 표시용.
 * id = 트리거 응답의 id 와 같은 값(멱등키 파생 결정적 UUID)이라 프론트가 접수 응답과 매칭할 수 있다.
 * reportType = 생성 유형(MORNING_BRIEFING|ON_DEMAND) — 접수 시점 값이라 항상 채워진다.
 * topic 은 접수 당시 검색 주제이며 status는 Agent Job과 Publish 반영 상태를 나타낸다.
 */
public record GenerationPendingResponse(
        UUID id,
        String topic,
        String contentType,
        String reportType,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String errorCode) {

    public static GenerationPendingResponse from(GenerationPending pending) {
        return new GenerationPendingResponse(
                pending.getId(),
                pending.getTopic(),
                pending.getContentType(),
                pending.getReportType(),
                pending.getStatus(),
                pending.getCreatedAt(),
                pending.getUpdatedAt(),
                pending.getErrorCode());
    }
}
