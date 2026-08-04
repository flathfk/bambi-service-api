package com.bambi.service.agent.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link AgentContextOutbox}의 lease·재시도·완료 상태 전이를 검증한다. */
class AgentContextOutboxTest {

    @Test
    void 실패하면_lease를_해제하고_지정된_시각까지_재시도를_미룬다() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-04T10:00:00+09:00");
        AgentContextOutbox outbox = new AgentContextOutbox(7L, 1, "{}", now);

        UUID token = outbox.claim("worker-1", now, Duration.ofSeconds(30));
        outbox.scheduleRetry(now, Duration.ofSeconds(5), "ApiException");

        assertThat(outbox.getStatus()).isEqualTo(AgentContextOutboxStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(now.plusSeconds(5));
        assertThat(outbox.getLastError()).isEqualTo("ApiException");
        assertThat(outbox.getLockedBy()).isNull();
        assertThat(outbox.getLockToken()).isNull();
        assertThat(outbox.getLockedUntil()).isNull();
        assertThat(outbox.isClaimedBy(token)).isFalse();
    }

    @Test
    void 재시도_claim은_시도횟수를_올리고_성공하면_published로_끝낸다() {
        OffsetDateTime first = OffsetDateTime.parse("2026-08-04T10:00:00+09:00");
        AgentContextOutbox outbox = new AgentContextOutbox(7L, 1, "{}", first);
        UUID firstToken = outbox.claim("worker-1", first, Duration.ofSeconds(30));
        outbox.scheduleRetry(first, Duration.ofSeconds(5), "ApiException");

        OffsetDateTime retryAt = first.plusSeconds(5);
        UUID secondToken = outbox.claim("worker-2", retryAt, Duration.ofSeconds(30));
        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(outbox.getAttemptCount()).isEqualTo(2);
        assertThat(outbox.isClaimedBy(secondToken)).isTrue();

        outbox.markPublished(retryAt.plusSeconds(1));

        assertThat(outbox.getStatus()).isEqualTo(AgentContextOutboxStatus.PUBLISHED);
        assertThat(outbox.getPublishedAt()).isEqualTo(retryAt.plusSeconds(1));
        assertThat(outbox.getLastError()).isNull();
        assertThat(outbox.getLockToken()).isNull();
    }
}
