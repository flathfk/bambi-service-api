package com.bambi.service.agent.outbox;

/** Agent 사용자 컨텍스트 Outbox의 전달 상태. */
public enum AgentContextOutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED
}
