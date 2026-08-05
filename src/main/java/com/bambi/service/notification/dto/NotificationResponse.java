package com.bambi.service.notification.dto;

import com.bambi.service.notification.Notification;

import java.time.OffsetDateTime;

/** 프론트 알림 Inbox 항목. */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String targetPath,
        boolean read,
        OffsetDateTime createdAt) {

    /** 알림 Entity를 API 응답으로 변환한다. */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetPath(),
                notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}
