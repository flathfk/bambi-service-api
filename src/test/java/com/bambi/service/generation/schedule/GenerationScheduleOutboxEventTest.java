package com.bambi.service.generation.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link GenerationScheduleOutboxEvent}의 lease·재시도·DEAD 상태 전이를 검증한다. */
class GenerationScheduleOutboxEventTest {

    @Test
    void Claim은_시도수를_늘리고_Worker_lease를_기록한다() {
        GenerationScheduleOutboxEvent event = event();

        event.markProcessing("worker-1", 120);

        assertThat(event.getStatus()).isEqualTo("PROCESSING");
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLockedBy()).isEqualTo("worker-1");
        assertThat(event.getLeaseExpiresAt()).isAfter(OffsetDateTime.now().plusSeconds(115));
        assertThat(event.isOwnedBy("worker-1")).isTrue();
        assertThat(event.isOwnedBy("worker-2")).isFalse();
    }

    @Test
    void 실패는_지수_지연으로_돌아가고_최대_시도에서는_DEAD가_된다() {
        GenerationScheduleOutboxEvent event = event();

        event.markProcessing("worker-1", 120);
        OffsetDateTime firstFailure = OffsetDateTime.now();
        event.markRetry(new RuntimeException("agent down"), 3, 5, 300);

        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getNextAttemptAt()).isAfterOrEqualTo(firstFailure.plusSeconds(5));
        assertThat(event.getLastError()).isEqualTo("agent down");

        event.markProcessing("worker-1", 120);
        event.markRetry(new RuntimeException("still down"), 3, 5, 300);
        assertThat(event.getStatus()).isEqualTo("PENDING");

        event.markProcessing("worker-1", 120);
        event.markRetry(new RuntimeException("last failure"), 3, 5, 300);
        assertThat(event.getStatus()).isEqualTo("DEAD");
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLeaseExpiresAt()).isNull();
    }

    @Test
    void 전달_완료는_lease를_비우고_Agent_Job을_보존한다() {
        GenerationScheduleOutboxEvent event = event();
        event.markProcessing("worker-1", 120);

        event.markDelivered("job-1");

        assertThat(event.getStatus()).isEqualTo("DELIVERED");
        assertThat(event.getAgentJobId()).isEqualTo("job-1");
        assertThat(event.getLockedBy()).isNull();
    }

    private GenerationScheduleOutboxEvent event() {
        return GenerationScheduleOutboxEvent.pending(
                GenerationSchedulePhase.BRIEFING_PREPARATION,
                LocalDate.of(2026, 8, 12),
                7L);
    }
}
