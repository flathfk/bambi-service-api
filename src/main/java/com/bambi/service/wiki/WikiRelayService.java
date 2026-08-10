package com.bambi.service.wiki;

import com.bambi.service.wiki.dto.WikiDocumentDetailResponse;
import com.bambi.service.wiki.dto.WikiDocumentsResponse;
import com.bambi.service.wiki.dto.WikiGraphResponse;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import com.bambi.service.wiki.dto.WikiTopNodesResponse;
import com.bambi.service.wiki.dto.WikiResetResponse;
import org.springframework.stereotype.Service;

/**
 * 개인 Wiki 조회 중계 — agent 응답을 사용자 화면용으로 다듬는다.
 * 문서 목록의 내부 schema를 제외하고 Graph·문서 상세는 인증 사용자 범위 그대로 통과시킨다.
 */
@Service
public class WikiRelayService {

    // 연결 상위 노드 limit 은 agent 계약상 1~100.
    private static final int MIN_TOP_NODES = 1;
    private static final int MAX_TOP_NODES = 100;

    private final AgentWikiClient wikiClient;

    public WikiRelayService(AgentWikiClient wikiClient) {
        this.wikiClient = wikiClient;
    }

    public WikiTagsResponse tags(long userId) {
        return wikiClient.getTags(userId);
    }

    /** 저장 자료 목록 — 내부 schema 문서는 화면에 안 보이게 제외한다. */
    public WikiDocumentsResponse documents(long userId) {
        return wikiClient.getDocuments(userId).withoutSchema();
    }

    /** 인증 사용자의 전체 LLM Wiki Graph를 그대로 중계한다. */
    public WikiGraphResponse graph(long userId) {
        return wikiClient.getGraph(userId);
    }

    /** 인증 사용자가 소유한 LLM Wiki 문서 상세를 중계한다. */
    public WikiDocumentDetailResponse document(long userId, String documentId) {
        return wikiClient.getDocument(userId, documentId);
    }

    /** 연결 상위 노드 — 범위를 벗어난 limit(0·음수·100 초과)은 계약 범위 1~100 으로 잘라 맞춘다. */
    public WikiTopNodesResponse topNodes(long userId, int limit) {
        int safeLimit = Math.min(MAX_TOP_NODES, Math.max(MIN_TOP_NODES, limit));
        return wikiClient.getTopNodes(userId, safeLimit);
    }

    /** 인증 사용자의 원본을 영구 삭제하고 개인 LLM Wiki 상태를 초기화한다. */
    public WikiResetResponse reset(long userId) {
        return wikiClient.reset(userId);
    }
}
