package com.bambi.service.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관심사 생성/수정 요청 — name 필수(1~100자, V1 스키마 length=100). */
public record InterestRequest(
        @NotBlank @Size(max = 100) String name) {
}
