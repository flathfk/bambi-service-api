package com.bambi.service.agent.dto;

import java.util.List;

/**
 * Agent 계약: POST /agent/cards/generate 응답.
 * docs/agent-contract.md 와 필드를 1:1 로 유지한다 (Contract Test 대상).
 */
public record CardGenerateResponse(List<GeneratedCard> cards) {

    public record GeneratedCard(
            String title,
            String summary,
            String whyForYou,
            List<Source> sources) {
    }

    /** 출처 — 카드는 반드시 출처를 갖는다 (출처 없는 답변 금지) */
    public record Source(String title, String url) {
    }
}
