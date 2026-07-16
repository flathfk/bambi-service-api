package com.bambi.service.agent;

import com.bambi.service.agent.dto.BookmarkProcessRequest;
import com.bambi.service.agent.dto.BookmarkProcessResponse;
import com.bambi.service.agent.dto.CardGenerateRequest;
import com.bambi.service.agent.dto.CardGenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * P0 하드코딩 Mock — FastAPI 를 기다리지 않고 저장→카드→피드를 관통하기 위한 고정 응답.
 * 계약(docs/agent-contract.md) 형태는 그대로 지키되 값만 고정이다.
 * 요청/응답 로그 규칙: P0 는 애플리케이션 로그로 남기고,
 * DB AI 로그(ai_request_logs/ai_response_logs) 적재는 실제 Gateway(P1, 소라)에서 붙인다.
 */
@Component
public class MockAgentClient implements AgentClient {

    private static final Logger log = LoggerFactory.getLogger(MockAgentClient.class);

    private static final String MOCK_SUMMARY =
            "저장한 콘텐츠의 핵심을 3줄로 정리한 Mock 요약입니다. "
                    + "실제 LLM 연동(P1) 전까지 고정 응답을 반환합니다.";
    private static final String MOCK_WHY_FOR_YOU =
            "회원님이 방금 저장한 콘텐츠와 같은 주제를 다루고 있어요. (Mock 추천 사유)";

    @Override
    public BookmarkProcessResponse processBookmark(BookmarkProcessRequest request) {
        log.info("[MockAgent] processBookmark 요청: bookmarkId={}, url={}", request.bookmarkId(), request.url());
        BookmarkProcessResponse response = new BookmarkProcessResponse(
                MOCK_SUMMARY,
                List.of("AI Agent", "LangGraph"),
                List.of("RAG", "Tool-use"),
                0.87);
        log.info("[MockAgent] processBookmark 응답: interests={}, confidence={}",
                response.interests(), response.confidence());
        return response;
    }

    @Override
    public CardGenerateResponse generateCards(CardGenerateRequest request) {
        log.info("[MockAgent] generateCards 요청: userId={}, bookmarks={}", request.userId(),
                request.bookmarks().size());
        // 출처 기반 원칙: 카드 출처는 실제 저장된 북마크(제목/URL)를 그대로 되돌려준다.
        List<CardGenerateResponse.Source> sources = request.bookmarks().stream()
                .map(b -> new CardGenerateResponse.Source(b.title(), b.url()))
                .toList();
        CardGenerateResponse.GeneratedCard card = new CardGenerateResponse.GeneratedCard(
                firstTitleOrDefault(request),
                MOCK_SUMMARY,
                MOCK_WHY_FOR_YOU,
                sources);
        log.info("[MockAgent] generateCards 응답: cards=1, sources={}", sources.size());
        return new CardGenerateResponse(List.of(card));
    }

    private String firstTitleOrDefault(CardGenerateRequest request) {
        return request.bookmarks().stream()
                .map(CardGenerateRequest.BookmarkPayload::title)
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .orElse("저장한 콘텐츠 브리핑");
    }
}
