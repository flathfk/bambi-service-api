package com.bambi.service.notification;

import com.bambi.service.auth.AuthPrincipal;
import com.bambi.service.common.response.ApiResponse;
import com.bambi.service.notification.dto.NotificationListResponse;
import com.bambi.service.notification.dto.NotificationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자의 알림 Inbox API. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 최근 알림 목록과 읽지 않은 개수를 반환한다. */
    @GetMapping
    public ApiResponse<NotificationListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(notificationService.list(principal.id()));
    }

    /** 알림 하나를 읽음 처리한다. */
    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(principal.id(), id));
    }
}
