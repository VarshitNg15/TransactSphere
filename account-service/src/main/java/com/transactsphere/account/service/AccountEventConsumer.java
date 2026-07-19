package com.transactsphere.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactsphere.account.dto.AccountUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final com.transactsphere.account.cache.AntiStampedeCache antiStampedeCache;

    @KafkaListener(topics = "account.updated", groupId = "${spring.kafka.consumer.group-id:account-service}")
    public void consumeAccountUpdatedEvent(String message) {
        try {
            AccountUpdatedEvent event = objectMapper.readValue(message, AccountUpdatedEvent.class);
            String accountNumber = event.getAccountNumber();

            log.info("Received account updated event for account {}. Action: {}. Evicting cache...", accountNumber, event.getAction());

            if (cacheManager.getCache("accounts") != null) {
                cacheManager.getCache("accounts").evict(accountNumber);
            }
            
            // Evict from new anti-stampede cache
            antiStampedeCache.evict("accounts::" + accountNumber);
            
            log.info("Successfully evicted cache for account {}", accountNumber);
        } catch (Exception e) {
            log.error("Failed to process account updated event: {}", e.getMessage());
        }
    }
}
