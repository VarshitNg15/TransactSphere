package com.transactsphere.notification.controller;

import com.transactsphere.notification.model.NotificationLog;
import com.transactsphere.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id, 
            @RequestHeader("X-User-Id") Long userId) {
        inAppNotificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(
            @RequestHeader("X-User-Id") Long userId) {
        inAppNotificationService.deleteAllMyNotifications(userId);
        return ResponseEntity.noContent().build();
    }
}
