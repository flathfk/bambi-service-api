package com.bambi.service.generation.schedule;

import com.bambi.service.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link GenerationSchedulePublisher}의 분산 중복 방지와 키셋 발행을 검증한다. */
class GenerationSchedulePublisherTest {

    private final GenerationScheduleRunRepository runRepository =
            mock(GenerationScheduleRunRepository.class);
    private final GenerationScheduleOutboxRepository outboxRepository =
            mock(GenerationScheduleOutboxRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final GenerationSchedulePublisher publisher = new GenerationSchedulePublisher(
            runRepository, outboxRepository, userRepository, 2);

    @Test
    void 다른_인스턴스가_발행_중이면_사용자를_읽지_않는다() {
        LocalDate date = LocalDate.of(2026, 8, 11);
        when(runRepository.tryAcquireLock(any())).thenReturn(false);

        GenerationSchedulePublisher.PublicationResult result = publisher.publish(
                GenerationSchedulePhase.BRIEFING_PREPARATION, date);

        assertThat(result.status()).isEqualTo(GenerationSchedulePublisher.PublicationStatus.ALREADY_RUNNING);
        verify(userRepository, never()).findActiveIdsAfter(any(Long.class), any(Pageable.class));
        verify(outboxRepository, never()).saveAll(any());
    }

    @Test
    void 이미_완료한_날짜와_단계는_다시_발행하지_않는다() {
        LocalDate date = LocalDate.of(2026, 8, 11);
        when(runRepository.tryAcquireLock(any())).thenReturn(true);
        when(runRepository.existsByPhaseAndScheduleDate(
                GenerationSchedulePhase.MORNING_GENERATION, date)).thenReturn(true);

        GenerationSchedulePublisher.PublicationResult result = publisher.publish(
                GenerationSchedulePhase.MORNING_GENERATION, date);

        assertThat(result.status()).isEqualTo(GenerationSchedulePublisher.PublicationStatus.ALREADY_PUBLISHED);
        verify(userRepository, never()).findActiveIdsAfter(any(Long.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 활성_사용자를_ID_키셋으로_읽어_사용자별_이벤트를_발행한다() {
        LocalDate date = LocalDate.of(2026, 8, 11);
        when(runRepository.tryAcquireLock(any())).thenReturn(true);
        when(runRepository.existsByPhaseAndScheduleDate(any(), eq(date))).thenReturn(false);
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findActiveIdsAfter(eq(0L), any(Pageable.class)))
                .thenReturn(List.of(3L, 8L));
        when(userRepository.findActiveIdsAfter(eq(8L), any(Pageable.class)))
                .thenReturn(List.of(21L));
        when(userRepository.findActiveIdsAfter(eq(21L), any(Pageable.class)))
                .thenReturn(List.of());

        GenerationSchedulePublisher.PublicationResult result = publisher.publish(
                GenerationSchedulePhase.BRIEFING_PREPARATION, date);

        assertThat(result.status()).isEqualTo(GenerationSchedulePublisher.PublicationStatus.PUBLISHED);
        assertThat(result.userCount()).isEqualTo(3);
        verify(userRepository, times(3)).findActiveIdsAfter(any(Long.class), any(Pageable.class));

        ArgumentCaptor<List<GenerationScheduleOutboxEvent>> batches = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository, times(2)).saveAll(batches.capture());
        assertThat(batches.getAllValues()).flatExtracting(batch -> batch)
                .extracting(GenerationScheduleOutboxEvent::getUserId)
                .containsExactly(3L, 8L, 21L);
        assertThat(batches.getAllValues()).flatExtracting(batch -> batch)
                .allMatch(event -> event.getPhase() == GenerationSchedulePhase.BRIEFING_PREPARATION)
                .allMatch(event -> date.equals(event.getScheduleDate()));
    }

    @Test
    void 페이지_크기는_양수여야_한다() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new GenerationSchedulePublisher(
                        runRepository, outboxRepository, userRepository, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
