package com.transactsphere.notification.repository;

import com.transactsphere.notification.model.NotificationLog;
import com.transactsphere.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserIdAndTypeOrderByTimestampDesc(Long userId, NotificationType type);
    void deleteByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
}
