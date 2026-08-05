package com.bambi.service.notification.dto;

import java.util.List;

/** 알림 목록과 읽지 않은 개수를 함께 반환한다. */
public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> items) {

    public NotificationListResponse {
        items = List.copyOf(items);
    }
}
