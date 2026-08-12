package com.bambi.service.briefing;

import java.util.List;

/** Agent의 날짜별 준비 여부와 개인 Wiki에서 선정한 아침 브리핑 주제. */
public record MorningBriefingTopics(boolean prepared, List<String> topics) {

    public MorningBriefingTopics {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }

    public static MorningBriefingTopics notPrepared() {
        return new MorningBriefingTopics(false, List.of());
    }

    public static MorningBriefingTopics ready(List<String> topics) {
        return new MorningBriefingTopics(true, topics);
    }
}
