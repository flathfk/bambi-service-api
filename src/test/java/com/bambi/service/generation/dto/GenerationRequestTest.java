package com.bambi.service.generation.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link GenerationRequest} — {@code topic} 과 {@code topics} 의 의미가 서로 뒤바뀌는 지점.
 *
 * <p>{@code topics} 가 있으면 {@code topic} 은 카드 제목용이고, 없으면 <b>실제 검색어</b>다.
 * 규칙이 정반대라 섞이면 고정 문구로 기사를 검색하게 된다(2026-08-05 유림 확인).
 */
class GenerationRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 단일_주제는_topics_를_직렬화에서_생략한다() throws Exception {
        GenerationRequest request = GenerationRequest.singleTopic(
                "key-1", "폭염", "interest_news_card", "ON_DEMAND");

        String json = mapper.writeValueAsString(request);

        // 빈 배열을 보내면 agent 가 "여러 주제" 경로로 오해할 여지가 없도록 아예 뺀다(회귀 없음).
        assertThat(json).doesNotContain("topics");
        assertThat(json).contains("\"topic\":\"폭염\"");
    }

    @Test
    void 여러_주제는_topic_과_topics_를_함께_보낸다() throws Exception {
        GenerationRequest request = GenerationRequest.multiTopic(
                "key-1", "오늘의 관심사 브리핑", List.of("폭염", "퇴근"),
                "interest_news_card", "MORNING_BRIEFING");

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"topic\":\"오늘의 관심사 브리핑\"");
        assertThat(json).contains("\"topics\":[\"폭염\",\"퇴근\"]");
    }

    @Test
    void 주제가_비면_제목용_문구만_남으므로_만들지_못하게_막는다() {
        // 이대로 나가면 "오늘의 관심사 브리핑" 이 실제 검색어가 되어 엉뚱한 기사를 물어온다.
        assertThatThrownBy(() -> GenerationRequest.multiTopic(
                "key-1", "오늘의 관심사 브리핑", List.of(), "interest_news_card", "MORNING_BRIEFING"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GenerationRequest.multiTopic(
                "key-1", "오늘의 관심사 브리핑", null, "interest_news_card", "MORNING_BRIEFING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 넘겨받은_주제_목록을_복사해_외부_변경에서_격리한다() {
        List<String> mutable = new java.util.ArrayList<>(List.of("폭염", "퇴근"));
        GenerationRequest request = GenerationRequest.multiTopic(
                "key-1", "오늘의 관심사 브리핑", mutable, "interest_news_card", "MORNING_BRIEFING");

        mutable.clear();

        // 호출부가 목록을 재사용해도 이미 만든 요청의 주제가 사라지면 안 된다.
        assertThat(request.topics()).containsExactly("폭염", "퇴근");
    }
}
