package com.bambi.service.agent.publish;

/**
 * Mock 전용 — "agent 가 생성한 발행 콘텐츠"를 인프로세스 큐에 밀어 넣는 통로.
 * 실제로는 agent-api 가 스케줄 생성 후 자기 DB 에 쌓는 부분이라 service 코드에 없다.
 * P0 관통을 위해 도메인(재처리 트리거)이 이 통로로 항목을 넣으면,
 * {@link com.bambi.service.worker.PublishPollingWorker} 가 폴링으로 집어 카드로 발행한다.
 * P1 에서 실제 agent 연동으로 바뀌면 이 인터페이스는 사라진다.
 */
public interface MockPublishInbox {

    /** 사용자에게 발행할 Mock 생성 카드 1건을 큐에 적재 (content_id 로 멱등). */
    void enqueue(Long userId, String contentId, String title,
                 String sourceTitle, String sourceUrl);
}
