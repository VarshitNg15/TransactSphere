package com.transactsphere.notification.service;

import com.transactsphere.notification.model.NotificationLog;
import com.transactsphere.notification.model.NotificationType;
import com.transactsphere.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public void saveInAppNotification(Long userId, String message) {
        NotificationLog logEntry = NotificationLog.builder()
                .userId(userId)
                .message(message)
                .type(NotificationType.IN_APP)
                .status("SENT") // In-App is essentially available instantly
                .build();
        notificationLogRepository.save(logEntry);
        log.info("Saved IN_APP notification for user {}", userId);
    }

    public List<NotificationLog> getInAppNotifications(Long userId) {
        return notificationLogRepository.findByUserIdAndTypeOrderByTimestampDesc(userId, NotificationType.IN_APP);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        notificationLogRepository.deleteByIdAndUserId(id, userId);
        log.info("Deleted notification {} for user {}", id, userId);
    }

    @Transactional
    public void deleteAllMyNotifications(Long userId) {
        notificationLogRepository.deleteByUserId(userId);
        log.info("Deleted all notifications for user {}", userId);
    }
}
