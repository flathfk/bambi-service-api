package com.bambi.service.agent;

import com.bambi.service.agent.dto.BookmarkProcessRequest;
import com.bambi.service.agent.dto.BookmarkProcessResponse;
import com.bambi.service.agent.dto.CardGenerateRequest;
import com.bambi.service.agent.dto.CardGenerateResponse;

/**
 * Service → Agent 호출 경계 (docs/agent-contract.md 의 동기 REST 계약).
 * P0 는 인프로세스 {@link MockAgentClient} 로 관통하고,
 * P1 에서 실제 FastAPI 를 호출하는 Gateway 구현(소라 담당)으로 교체한다.
 * 교체 시 이 인터페이스(=계약)만 지키면 도메인 코드는 손대지 않는다.
 */
public interface AgentClient {

    /** 북마크 요약/관심사 추론 (계약: POST /agent/bookmarks/process) */
    BookmarkProcessResponse processBookmark(BookmarkProcessRequest request);

    /** 브리핑 카드 생성 (계약: POST /agent/cards/generate) */
    CardGenerateResponse generateCards(CardGenerateRequest request);
}
