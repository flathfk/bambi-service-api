package com.bambi.service.onboarding;

import com.bambi.service.interest.Interest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 온보딩 관심사에서 최대 3개의 리포트 주제를 결정적으로 선정한다. */
@Component
public class OnboardingReportSelector {

    public static final int MAX_REPORTS = 3;

    /**
     * 사용자 입력 Topic을 선택 순서대로 먼저 고른다. 남은 자리는 선택 Topic 수가 많은
     * Category 순으로 채우며, Category당 가장 먼저 선택한 Topic 하나만 사용한다.
     * Category 수가 같으면 해당 Category에서 첫 Topic을 선택한 순서가 빠른 쪽이 우선이다.
     */
    public List<Interest> select(List<Interest> orderedInterests) {
        List<Interest> selected = new ArrayList<>(MAX_REPORTS);
        Map<String, CategoryCandidate> categories = new LinkedHashMap<>();

        for (int index = 0; index < orderedInterests.size(); index++) {
            Interest interest = orderedInterests.get(index);
            if (isCustomTopic(interest)) {
                if (selected.size() < MAX_REPORTS) {
                    selected.add(interest);
                }
                continue;
            }
            int selectionIndex = index;
            categories.compute(
                    interest.getTaxonomyCategoryId(),
                    (ignored, current) -> current == null
                            ? new CategoryCandidate(interest, selectionIndex, 1)
                            : current.increment());
        }

        categories.values().stream()
                .sorted(Comparator
                        .comparingInt(CategoryCandidate::topicCount).reversed()
                        .thenComparingInt(CategoryCandidate::firstSelectionIndex))
                .map(CategoryCandidate::firstTopic)
                .limit(MAX_REPORTS - selected.size())
                .forEach(selected::add);
        return List.copyOf(selected);
    }

    private boolean isCustomTopic(Interest interest) {
        return interest.getTaxonomyCategoryId() == null || interest.getTaxonomyTopicId() == null;
    }

    private record CategoryCandidate(
            Interest firstTopic,
            int firstSelectionIndex,
            int topicCount) {

        private CategoryCandidate increment() {
            return new CategoryCandidate(firstTopic, firstSelectionIndex, topicCount + 1);
        }
    }
}
