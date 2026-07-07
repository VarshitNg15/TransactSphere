package com.transactsphere.analytics.service;

import com.transactsphere.analytics.client.AccountClient;
import com.transactsphere.analytics.dto.AccountResponse;
import com.transactsphere.analytics.dto.TransactionEvent;
import com.transactsphere.analytics.model.UserAnalytics;
import com.transactsphere.analytics.repository.UserAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserAnalyticsRepository userAnalyticsRepository;
    private final AccountClient accountClient;

    @KafkaListener(topics = "transaction.completed", groupId = "analytics-group")
    public void consumeTransactionCompleted(TransactionEvent event) {
        log.info("Received transaction.completed event in analytics-service: {}", event.getTransactionId());
        try {
            BigDecimal amount = event.getAmount();
            String type = event.getTransactionType();
            LocalDateTime timestamp = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();

            if ("DEPOSIT".equalsIgnoreCase(type)) {
                Long targetUserId = getUserIdForAccount(event.getTargetAccountId());
                if (targetUserId != null) {
                    updateAnalyticsForUser(targetUserId, amount, type, timestamp);
                }
            } else if ("WITHDRAWAL".equalsIgnoreCase(type)) {
                Long sourceUserId = getUserIdForAccount(event.getSourceAccountId());
                if (sourceUserId != null) {
                    updateAnalyticsForUser(sourceUserId, amount, type, timestamp);
                }
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                Long sourceUserId = getUserIdForAccount(event.getSourceAccountId());
                if (sourceUserId != null) {
                    updateAnalyticsForUser(sourceUserId, amount, type, timestamp);
                }
                Long targetUserId = getUserIdForAccount(event.getTargetAccountId());
                if (targetUserId != null) {
                    updateAnalyticsForUser(targetUserId, amount, type, timestamp);
                }
            }
        } catch (Exception e) {
            log.error("Error processing transaction completed event in analytics: {}", e.getMessage(), e);
        }
    }

    private void updateAnalyticsForUser(Long userId, BigDecimal amount, String type, LocalDateTime timestamp) {
        UserAnalytics ua = userAnalyticsRepository.findById(userId)
                .orElse(UserAnalytics.builder()
                        .userId(userId)
                        .totalVolume(BigDecimal.ZERO)
                        .totalCount(0L)
                        .depositVolume(BigDecimal.ZERO)
                        .withdrawalVolume(BigDecimal.ZERO)
                        .transferVolume(BigDecimal.ZERO)
                        .build());

        ua.setTotalVolume(ua.getTotalVolume().add(amount));
        ua.setTotalCount(ua.getTotalCount() + 1);
        ua.setLastTransactionTimestamp(timestamp);

        if ("DEPOSIT".equalsIgnoreCase(type)) {
            ua.setDepositVolume(ua.getDepositVolume().add(amount));
        } else if ("WITHDRAWAL".equalsIgnoreCase(type)) {
            ua.setWithdrawalVolume(ua.getWithdrawalVolume().add(amount));
        } else if ("TRANSFER".equalsIgnoreCase(type)) {
            ua.setTransferVolume(ua.getTransferVolume().add(amount));
        }

        userAnalyticsRepository.save(ua);
        log.info("Updated analytics for user ID: {}", userId);
    }

    private Long getUserIdForAccount(String accountNumber) {
        if (accountNumber == null) return null;
        try {
            AccountResponse response = accountClient.getAccountInternal(accountNumber);
            return response != null ? response.getUserId() : null;
        } catch (Exception e) {
            log.error("Failed to get userId for account: {}. Error: {}", accountNumber, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        BigDecimal totalVolume = userAnalyticsRepository.sumTotalVolume();
        Long totalCount = userAnalyticsRepository.sumTotalCount();
        Long activeUsers = userAnalyticsRepository.countActiveUsers();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVolume", totalVolume != null ? totalVolume : BigDecimal.ZERO);
        stats.put("totalCount", totalCount != null ? totalCount : 0L);
        stats.put("activeUsers", activeUsers != null ? activeUsers : 0L);
        return stats;
    }

    @Transactional(readOnly = true)
    public UserAnalytics getUserAnalytics(Long userId) {
        return userAnalyticsRepository.findById(userId)
                .orElse(UserAnalytics.builder()
                        .userId(userId)
                        .totalVolume(BigDecimal.ZERO)
                        .totalCount(0L)
                        .depositVolume(BigDecimal.ZERO)
                        .withdrawalVolume(BigDecimal.ZERO)
                        .transferVolume(BigDecimal.ZERO)
                        .build());
    }
}
