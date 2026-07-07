package com.transactsphere.audit.service;

import com.transactsphere.audit.dto.TransactionEvent;
import com.transactsphere.audit.model.AuditLog;
import com.transactsphere.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "transaction.completed", groupId = "audit-group")
    public void consumeTransactionCompleted(TransactionEvent event) {
        log.info("Auditing completed transaction: {}", event.getTransactionId());
        
        AuditLog auditLog = AuditLog.builder()
                .eventType("TRANSACTION_COMPLETED")
                .message(String.format("Transaction %s of type %s completed. Source: %s, Target: %s, Amount: %s",
                        event.getTransactionId(), event.getTransactionType(),
                        event.getSourceAccountId(), event.getTargetAccountId(), event.getAmount()))
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .serviceName("transaction-service")
                .build();

        auditLogRepository.save(auditLog);
    }

    @KafkaListener(topics = "transaction.fraudulent", groupId = "audit-group")
    public void consumeTransactionFraudulent(TransactionEvent event) {
        log.warn("Auditing suspicious transaction blocked: {}", event.getTransactionId());
        
        AuditLog auditLog = AuditLog.builder()
                .eventType("TRANSACTION_FRAUDULENT")
                .message(String.format("Transaction %s of type %s flagged as fraudulent. Reason: %s. Source: %s, Target: %s, Amount: %s",
                        event.getTransactionId(), event.getTransactionType(), event.getFraudReason(),
                        event.getSourceAccountId(), event.getTargetAccountId(), event.getAmount()))
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .serviceName("transaction-service")
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog logCustomEvent(String eventType, String message, Long userId, String serviceName) {
        AuditLog auditLog = AuditLog.builder()
                .eventType(eventType)
                .message(message)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .serviceName(serviceName != null ? serviceName : "unknown")
                .build();
        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
