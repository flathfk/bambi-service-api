package com.bambi.service.generation.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link GenerationScheduleOutboxService}의 Claim과 Worker 소유권 검사를 검증한다. */
class GenerationScheduleOutboxServiceTest {

    private final GenerationScheduleOutboxRepository repository =
            mock(GenerationScheduleOutboxRepository.class);
    private final GenerationScheduleOutboxService service =
            new GenerationScheduleOutboxService(repository);

    @Test
    void Claim한_Batch에_Worker와_lease를_기록한다() {
        GenerationScheduleOutboxEvent event = event();
        when(repository.findClaimable(20)).thenReturn(List.of(event));

        List<GenerationScheduleOutboxEvent> claimed = service.claim(20, "worker-1", 120);

        assertThat(claimed).containsExactly(event);
        assertThat(event.getStatus()).isEqualTo("PROCESSING");
        assertThat(event.getLockedBy()).isEqualTo("worker-1");
        verify(repository).findClaimable(20);
    }

    @Test
    void lease를_가진_Worker만_이벤트를_종결할_수_있다() {
        GenerationScheduleOutboxEvent event = event();
        event.markProcessing("worker-1", 120);
        when(repository.findById(3L)).thenReturn(Optional.of(event));

        assertThat(service.markDelivered(3L, "worker-2", "job-2")).isFalse();
        assertThat(event.getStatus()).isEqualTo("PROCESSING");

        assertThat(service.markDelivered(3L, "worker-1", "job-1")).isTrue();
        assertThat(event.getStatus()).isEqualTo("DELIVERED");
        assertThat(event.getAgentJobId()).isEqualTo("job-1");
    }

    private GenerationScheduleOutboxEvent event() {
        return GenerationScheduleOutboxEvent.pending(
                GenerationSchedulePhase.MORNING_GENERATION,
                LocalDate.of(2026, 8, 12),
                7L);
    }
}
