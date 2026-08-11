package com.bambi.service.generation.schedule;

import com.bambi.service.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 활성 사용자를 키셋으로 읽어 날짜·단계별 Outbox 이벤트를 원자적으로 발행한다. */
@Service
public class GenerationSchedulePublisher {

    private final GenerationScheduleRunRepository runRepository;
    private final GenerationScheduleOutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final int userPageSize;

    public GenerationSchedulePublisher(
            GenerationScheduleRunRepository runRepository,
            GenerationScheduleOutboxRepository outboxRepository,
            UserRepository userRepository,
            @Value("${app.scheduler.generation-outbox.user-page-size:500}") int userPageSize) {
        if (userPageSize < 1) {
            throw new IllegalArgumentException("user-page-size는 1 이상이어야 한다.");
        }
        this.runRepository = runRepository;
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.userPageSize = userPageSize;
    }

    /** 같은 날짜·단계는 한 인스턴스만 한 번 발행하고 결과 상태와 건수를 반환한다. */
    @Transactional
    public PublicationResult publish(GenerationSchedulePhase phase, LocalDate scheduleDate) {
        String lockKey = "generation-schedule:" + phase + ":" + scheduleDate;
        if (!runRepository.tryAcquireLock(lockKey)) {
            return PublicationResult.alreadyRunning();
        }
        if (runRepository.existsByPhaseAndScheduleDate(phase, scheduleDate)) {
            return PublicationResult.alreadyPublished();
        }

        GenerationScheduleRun run = runRepository.save(GenerationScheduleRun.start(phase, scheduleDate));
        long cursor = 0L;
        int published = 0;
        while (true) {
            List<Long> userIds = userRepository.findActiveIdsAfter(
                    cursor, PageRequest.of(0, userPageSize));
            if (userIds.isEmpty()) {
                break;
            }
            List<GenerationScheduleOutboxEvent> events = userIds.stream()
                    .map(userId -> GenerationScheduleOutboxEvent.pending(phase, scheduleDate, userId))
                    .toList();
            outboxRepository.saveAll(events);
            published += events.size();
            cursor = userIds.getLast();
        }
        run.markPublished(published);
        return PublicationResult.published(published);
    }

    /** 분산 발행 시도 결과. */
    public record PublicationResult(PublicationStatus status, int userCount) {
        static PublicationResult published(int userCount) {
            return new PublicationResult(PublicationStatus.PUBLISHED, userCount);
        }

        static PublicationResult alreadyRunning() {
            return new PublicationResult(PublicationStatus.ALREADY_RUNNING, 0);
        }

        static PublicationResult alreadyPublished() {
            return new PublicationResult(PublicationStatus.ALREADY_PUBLISHED, 0);
        }
    }

    /** Outbox 발행 시도의 종결 상태. */
    public enum PublicationStatus {
        PUBLISHED,
        ALREADY_RUNNING,
        ALREADY_PUBLISHED
    }
}
