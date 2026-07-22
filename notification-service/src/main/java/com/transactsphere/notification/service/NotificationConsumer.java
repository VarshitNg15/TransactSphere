package com.transactsphere.notification.service;

import com.transactsphere.notification.client.AccountClient;
import com.transactsphere.notification.client.UserClient;
import com.transactsphere.notification.dto.AccountResponse;
import com.transactsphere.notification.dto.TransactionEvent;
import com.transactsphere.notification.dto.UserProfileResponse;
import com.transactsphere.notification.model.NotificationLog;
import com.transactsphere.notification.model.NotificationType;
import com.transactsphere.notification.model.ProcessedEvent;
import com.transactsphere.notification.repository.NotificationLogRepository;
import com.transactsphere.notification.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final AccountClient accountClient;
    private final UserClient userClient;
    private final EmailService emailService;
    private final SmsService smsService;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction.completed", groupId = "notification-group")
    public void consumeTransactionEvent(String eventJson) {
        try {
            TransactionEvent event = objectMapper.readValue(eventJson, TransactionEvent.class);
            String eventId = "transaction.completed:" + event.getTransactionId();
            if (processedEventRepository.findById(eventId).isPresent()) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }
            log.info("Received transaction.completed event: {}", event.getTransactionId());
            // Determine which accounts to notify
            String[] accountsToNotify;
            if ("DEPOSIT".equals(event.getTransactionType())) {
                accountsToNotify = new String[]{event.getTargetAccountId()};
            } else if ("WITHDRAWAL".equals(event.getTransactionType())) {
                accountsToNotify = new String[]{event.getSourceAccountId()};
            } else {
                accountsToNotify = new String[]{event.getSourceAccountId(), event.getTargetAccountId()};
            }

            for (String accountNum : accountsToNotify) {
                if (accountNum == null) continue;

                // 1. Fetch Account Details
                AccountResponse account = accountClient.getAccountInternal(accountNum);
                if (account == null) {
                    log.error("Account not found for accountNumber: {}", accountNum);
                    continue;
                }

                // 2. Fetch User Profile
                UserProfileResponse user = userClient.getUserInternal(account.getUserId());
                if (user == null) {
                    log.error("User not found for userId: {}", account.getUserId());
                    continue;
                }

            // 3. Prepare Notification Content
            String subject = "TransactSphere - Transaction Completed";
            String message = String.format("Dear %s,\n\nA %s transaction of amount %s via %s has been successfully completed.\nTransaction ID: %s",
                    user.getUsername(),
                    event.getTransactionType(), event.getAmount(), event.getChannel(),
                    event.getTransactionId());

            // 4. Dispatch Email
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                boolean emailSent = emailService.sendEmail(user.getEmail(), subject, message);
                saveLog(user.getId(), message, NotificationType.EMAIL, emailSent ? "SENT" : "FAILED");
            }

            // 5. Dispatch SMS
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                boolean smsSent = smsService.sendSms(user.getPhoneNumber(), message);
                saveLog(user.getId(), message, NotificationType.SMS, smsSent ? "SENT" : "FAILED");
            }

            // 6. Dispatch In-App Notification
            inAppNotificationService.saveInAppNotification(user.getId(), message);

            } // Close for loop

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .status("COMPLETED")
                    .build());

        } catch (Exception e) {
            log.error("Error processing transaction event", e);
        }
    }

    @KafkaListener(topics = "transaction.fraudulent", groupId = "notification-group")
    public void consumeFraudEvent(String eventJson) {
        try {
            TransactionEvent event = objectMapper.readValue(eventJson, TransactionEvent.class);
            String eventId = "transaction.fraudulent:" + event.getTransactionId();
            if (processedEventRepository.findById(eventId).isPresent()) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }
            log.warn("Received transaction.fraudulent event: {}", event.getTransactionId());
            // Determine which accounts to notify
            String[] accountsToNotify;
            if ("DEPOSIT".equals(event.getTransactionType())) {
                accountsToNotify = new String[]{event.getTargetAccountId()};
            } else if ("WITHDRAWAL".equals(event.getTransactionType())) {
                accountsToNotify = new String[]{event.getSourceAccountId()};
            } else {
                accountsToNotify = new String[]{event.getSourceAccountId(), event.getTargetAccountId()};
            }

            for (String accountNum : accountsToNotify) {
                if (accountNum == null) continue;

                // 1. Fetch Account Details
                AccountResponse account = accountClient.getAccountInternal(accountNum);
                if (account == null) {
                    log.error("Account not found for accountNumber: {}", accountNum);
                    continue;
                }

                // 2. Fetch User Profile
                UserProfileResponse user = userClient.getUserInternal(account.getUserId());
                if (user == null) {
                    log.error("User not found for userId: {}", account.getUserId());
                    continue;
                }

                // 3. Prepare Notification Content
                String subject = "URGENT: TransactSphere - Suspicious Activity Blocked";
                String message = String.format("Dear %s,\n\nA suspicious %s transaction of amount %s via %s was attempted on your account and has been BLOCKED.\nReason: %s\nTransaction ID: %s. Please contact support immediately if this was not you.",
                        user.getUsername(),
                        event.getTransactionType(), event.getAmount(), event.getChannel(),
                        event.getFraudReason() != null ? event.getFraudReason() : "Suspicious Activity",
                        event.getTransactionId());

                // 4. Dispatch Email
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    boolean emailSent = emailService.sendEmail(user.getEmail(), subject, message);
                    saveLog(user.getId(), message, NotificationType.EMAIL, emailSent ? "SENT" : "FAILED");
                }

                // 5. Dispatch SMS
                if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                    boolean smsSent = smsService.sendSms(user.getPhoneNumber(), message);
                    saveLog(user.getId(), message, NotificationType.SMS, smsSent ? "SENT" : "FAILED");
                }

                // 6. Dispatch In-App Notification
                inAppNotificationService.saveInAppNotification(user.getId(), message);

            } // Close for loop

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .status("COMPLETED")
                    .build());

        } catch (Exception e) {
            log.error("Error processing fraud event", e);
        }
    }

    @KafkaListener(topics = "notification.generic", groupId = "notification-group")
    public void consumeGenericEvent(String eventJson) {
        try {
            com.transactsphere.notification.dto.GenericEvent event = objectMapper.readValue(eventJson, com.transactsphere.notification.dto.GenericEvent.class);
            String eventId = "notification.generic:" + event.getUserId() + ":" + Math.abs(event.getMessage().hashCode());
            if (processedEventRepository.findById(eventId).isPresent()) {
                log.info("Event {} already processed. Skipping.", eventId);
                return;
            }
            log.info("Received generic notification for user {}", event.getUserId());
            
            UserProfileResponse user = userClient.getUserInternal(event.getUserId());
            if (user == null) {
                log.error("User not found for userId: {}", event.getUserId());
                return;
            }

            String subject = "TransactSphere Notification";
            String message = event.getMessage();

            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                boolean emailSent = emailService.sendEmail(user.getEmail(), subject, message);
                saveLog(user.getId(), message, NotificationType.EMAIL, emailSent ? "SENT" : "FAILED");
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                boolean smsSent = smsService.sendSms(user.getPhoneNumber(), message);
                saveLog(user.getId(), message, NotificationType.SMS, smsSent ? "SENT" : "FAILED");
            }
            inAppNotificationService.saveInAppNotification(user.getId(), message);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .status("COMPLETED")
                    .build());

        } catch (Exception e) {
            log.error("Error processing generic event", e);
        }
    }

    private void saveLog(Long userId, String message, NotificationType type, String status) {
        NotificationLog logEntry = NotificationLog.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .status(status)
                .build();
        notificationLogRepository.save(logEntry);
    }
}
