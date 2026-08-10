package com.bambi.service.briefing.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 아침 브리핑 주제 선택 저장 (PUT /api/users/me/briefing-topics) — 전체 교체.
 *
 * <p>빈 배열은 정상이다("선택 해제" → 등록 관심사 폴백으로 돌아간다).
 * 다만 <b>필드 자체는 필수</b>다 — 프론트가 실수로 빼먹었을 때 저장된 선택이 조용히 지워지면
 * 안 되기 때문이다. "지우겠다"는 뜻이면 빈 배열을 명시적으로 보내야 한다.
 *
 * <p>개수·길이 상한은 여기 두지 않고 {@code BriefingTopicService} 가 <b>정리한 뒤에</b> 센다.
 * 여기서 원본 크기로 막으면 빈 문자열·중복 같은 노이즈가 상한에 포함돼, 사용자가 실제로는
 * 3개 이하를 골랐는데도 거절된다. 규칙이 두 군데로 갈리지 않도록 한 곳에만 둔다.
 */
public record BriefingTopicsRequest(@NotNull List<String> topics) {
}
