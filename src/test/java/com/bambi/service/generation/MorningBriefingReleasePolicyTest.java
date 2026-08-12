package com.bambi.service.generation;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link MorningBriefingReleasePolicy}의 정기·즉시 생성 분리를 검증한다. */
class MorningBriefingReleasePolicyTest {

    private final MorningBriefingReleasePolicy policy = new MorningBriefingReleasePolicy(7);
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-12T05:30:00+09:00");

    @Test
    void 정기_아침_브리핑은_해당일_07시에_공개한다() {
        OffsetDateTime availableAt = policy.availableAt(
                "MORNING_BRIEFING",
                "2026-08-12-40-interest_news_card-delta",
                now);

        assertThat(availableAt).isEqualTo("2026-08-12T07:00:00+09:00");
    }

    @Test
    void 개발용_아침_브리핑_즉시_생성은_기다리지_않는다() {
        OffsetDateTime availableAt = policy.availableAt(
                "MORNING_BRIEFING", "dev-morning-40-random", now);

        assertThat(availableAt).isEqualTo(now);
    }

    @Test
    void 온디맨드_생성은_정기키_모양이어도_기다리지_않는다() {
        OffsetDateTime availableAt = policy.availableAt(
                "ON_DEMAND", "2026-08-12-40-interest_news_card", now);

        assertThat(availableAt).isEqualTo(now);
    }
}
