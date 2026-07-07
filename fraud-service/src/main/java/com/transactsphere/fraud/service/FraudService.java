package com.transactsphere.fraud.service;

import com.transactsphere.fraud.client.AccountClient;
import com.transactsphere.fraud.dto.TransactionEvent;
import com.transactsphere.fraud.model.FraudLog;
import com.transactsphere.fraud.repository.FraudLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudLogRepository fraudLogRepository;
    private final AccountClient accountClient;

    @KafkaListener(topics = "transaction.fraudulent", groupId = "fraud-group")
    public void consumeFraudulentEvent(TransactionEvent event) {
        log.info("Received transaction.fraudulent event for transaction ID: {}", event.getTransactionId());
        
        FraudLog fraudLog = FraudLog.builder()
                .transactionId(event.getTransactionId())
                .sourceAccountNumber(event.getSourceAccountId())
                .targetAccountNumber(event.getTargetAccountId())
                .amount(event.getAmount())
                .transactionType(event.getTransactionType())
                .channel(event.getChannel())
                .status(event.getStatus())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .fraudReason(event.getFraudReason())
                .resolved(false)
                .build();

        fraudLogRepository.save(fraudLog);
        log.info("Successfully persisted fraud log for transaction: {}", event.getTransactionId());
    }

    @Transactional(readOnly = true)
    public List<FraudLog> getAllFraudLogs() {
        return fraudLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FraudLog> getFraudLogsByUser(Long userId) {
        try {
            List<String> accounts = accountClient.getAccountNumbersByUserInternal(userId);
            if (accounts == null || accounts.isEmpty()) {
                return Collections.emptyList();
            }
            return fraudLogRepository.findBySourceAccountNumberInOrTargetAccountNumberInOrderByTimestampDesc(accounts, accounts);
        } catch (Exception e) {
            log.error("Failed to fetch accounts for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public FraudLog resolveFraudLog(Long id, String resolvedBy) {
        FraudLog logEntry = fraudLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud log not found with ID: " + id));
        logEntry.setResolved(true);
        logEntry.setResolvedAt(LocalDateTime.now());
        logEntry.setResolvedBy(resolvedBy != null ? resolvedBy : "SYSTEM");
        return fraudLogRepository.save(logEntry);
    }
}
