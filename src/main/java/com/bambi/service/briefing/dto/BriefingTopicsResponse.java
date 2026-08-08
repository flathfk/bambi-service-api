package com.bambi.service.briefing.dto;

import java.util.List;

/**
 * 아침 브리핑 주제 선택 응답 — 조회·저장 공용.
 *
 * <p>미선택이면 {@code topics} 가 빈 배열이다(404 가 아니다). 프론트는 이 값으로 선택 상태를
 * 복구하고, 비어 있으면 "직접 고르지 않으면 내 관심사로 자동 발행" 안내를 띄운다.
 *
 * <p>저장 응답은 <b>정규화된 결과</b>다 — 공백을 다듬고 중복을 합친 뒤의 값이라
 * 프론트가 보낸 것과 다를 수 있다. 화면은 이 응답으로 상태를 갱신하면 된다.
 */
public record BriefingTopicsResponse(List<String> topics) {

    public static BriefingTopicsResponse of(List<String> topics) {
        return new BriefingTopicsResponse(topics);
    }
}
