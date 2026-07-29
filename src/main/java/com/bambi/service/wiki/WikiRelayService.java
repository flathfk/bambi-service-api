package com.bambi.service.wiki;

import com.bambi.service.wiki.dto.WikiDocumentsResponse;
import com.bambi.service.wiki.dto.WikiTagsResponse;
import com.bambi.service.wiki.dto.WikiTopNodesResponse;
import org.springframework.stereotype.Service;

/**
 * 개인 Wiki 조회 중계 — agent 응답을 사용자 화면용으로 다듬는다.
 * 지금은 문서 목록에서 내부 schema 문서를 걸러내는 정도만 하고, 나머지는 그대로 통과시킨다.
 */
@Service
public class WikiRelayService {

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

    public WikiTopNodesResponse topNodes(long userId, int limit) {
        return wikiClient.getTopNodes(userId, limit);
    }
}
