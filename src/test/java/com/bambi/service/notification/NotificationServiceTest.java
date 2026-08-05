package com.bambi.service.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** {@link NotificationService}의 리포트 완료 알림 계약을 검증한다. */
class NotificationServiceTest {

    @Test
    void 리포트_완료_알림은_멱등키와_리포트_경로를_저장한다() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(repository);
        UUID reportId = UUID.randomUUID();

        service.notifyReportReady(7L, "content-1", 2, "제목", "요약", reportId);

        verify(repository).insertReportReady(
                7L,
                "report-ready:content-1:v2",
                "새 리포트가 준비됐어요: 제목",
                "요약",
                "/report/" + reportId);
    }

    @Test
    void 긴_Agent_문자열은_알림_컬럼_길이에_맞춰_제한한다() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(repository);
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        service.notifyReportReady(
                7L, "content-1", 1, "가".repeat(500), "나".repeat(700), UUID.randomUUID());

        verify(repository).insertReportReady(
                eq(7L), anyString(), title.capture(), body.capture(), anyString());
        assertThat(title.getValue()).hasSize(200);
        assertThat(body.getValue()).hasSize(500);
    }
}
