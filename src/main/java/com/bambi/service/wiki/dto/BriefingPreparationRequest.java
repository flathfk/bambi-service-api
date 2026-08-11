package com.bambi.service.wiki.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Agent에 날짜별 아침 브리핑 주제·근거 준비를 요청하는 본문. */
public record BriefingPreparationRequest(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("briefing_date") LocalDate briefingDate,
        @JsonProperty("idempotency_key") String idempotencyKey,
        int limit) {
}
