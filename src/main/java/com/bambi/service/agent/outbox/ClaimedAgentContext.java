package com.bambi.service.agent.outbox;

import java.util.UUID;

/** HTTP 전송에 필요한, 트랜잭션 밖으로 꺼낸 Outbox claim 불변값. */
public record ClaimedAgentContext(
        long outboxId,
        long userId,
        String payload,
        int attemptCount,
        UUID lockToken) {
}
