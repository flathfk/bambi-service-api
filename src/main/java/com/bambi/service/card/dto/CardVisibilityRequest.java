package com.bambi.service.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 카드 공개설정 변경 요청. 허용값은 PUBLIC / PRIVATE (V1 CHECK 제약과 일치).
 * 잘못된 값은 @Valid 단계에서 400(VALIDATION_ERROR)으로 걸러진다.
 */
public record CardVisibilityRequest(
        @NotBlank
        @Pattern(regexp = "PUBLIC|PRIVATE", message = "visibility 는 PUBLIC 또는 PRIVATE 여야 합니다.")
        String visibility) {
}
