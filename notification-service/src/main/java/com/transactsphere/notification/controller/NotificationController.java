package com.transactsphere.notification.controller;

import com.transactsphere.notification.model.NotificationLog;
import com.transactsphere.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;

    @GetMapping
    public ResponseEntity<List<NotificationLog>> getMyNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        List<NotificationLog> notifications = inAppNotificationService.getInAppNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
}
