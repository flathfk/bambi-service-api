package com.bambi.service.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/** 관심사 생성/수정 요청. taxonomy 선택은 version·topicId를 함께 보내고 직접 입력은 둘 다 생략한다. */
public record InterestRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String taxonomyVersion,
        @Size(max = 50) String topicId) {

    /** taxonomy 식별자는 둘 다 있거나 둘 다 없어야 한다. */
    @AssertTrue(message = "taxonomyVersion과 topicId는 함께 전달해야 합니다.")
    public boolean isTaxonomySelectionValid() {
        return (taxonomyVersion == null) == (topicId == null);
    }

    /** 정식 taxonomy 토픽을 선택한 요청인지 반환한다. */
    public boolean isTaxonomySelection() {
        return taxonomyVersion != null && topicId != null;
    }
}
