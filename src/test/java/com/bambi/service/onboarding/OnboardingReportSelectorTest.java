package com.bambi.service.onboarding;

import com.bambi.service.interest.Interest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link OnboardingReportSelector}의 우선순위·Category당 1개·최대 3개 규칙을 검증한다. */
class OnboardingReportSelectorTest {

    private final OnboardingReportSelector selector = new OnboardingReportSelector();

    @Test
    void 사용자입력_topic을_먼저_고르고_남은자리는_최다_category로_채운다() {
        Interest a1 = taxonomy("A-첫째", "category-a", "a-1");
        Interest custom1 = custom("직접 입력 1");
        Interest b1 = taxonomy("B-첫째", "category-b", "b-1");
        Interest a2 = taxonomy("A-둘째", "category-a", "a-2");
        Interest custom2 = custom("직접 입력 2");
        Interest b2 = taxonomy("B-둘째", "category-b", "b-2");
        Interest b3 = taxonomy("B-셋째", "category-b", "b-3");

        List<Interest> selected = selector.select(
                List.of(a1, custom1, b1, a2, custom2, b2, b3));

        assertThat(selected).containsExactly(custom1, custom2, b1);
    }

    @Test
    void category별_첫_topic만_고르고_동률은_먼저_선택한_category가_앞선다() {
        Interest b1 = taxonomy("B-첫째", "category-b", "b-1");
        Interest a1 = taxonomy("A-첫째", "category-a", "a-1");
        Interest a2 = taxonomy("A-둘째", "category-a", "a-2");
        Interest b2 = taxonomy("B-둘째", "category-b", "b-2");
        Interest c1 = taxonomy("C-첫째", "category-c", "c-1");

        List<Interest> selected = selector.select(List.of(b1, a1, a2, b2, c1));

        assertThat(selected).containsExactly(b1, a1, c1);
    }

    @Test
    void 사용자입력_topic만_세개를_넘으면_먼저_선택한_세개만_고른다() {
        Interest first = custom("직접 입력 1");
        Interest second = custom("직접 입력 2");
        Interest third = custom("직접 입력 3");
        Interest fourth = custom("직접 입력 4");

        assertThat(selector.select(List.of(first, second, third, fourth)))
                .containsExactly(first, second, third);
    }

    private static Interest custom(String name) {
        return new Interest(1L, name);
    }

    private static Interest taxonomy(String name, String categoryId, String topicId) {
        return Interest.fromTaxonomy(1L, name, "1.0.0", categoryId, topicId);
    }
}
