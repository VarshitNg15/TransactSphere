package com.transactsphere.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    public boolean sendSms(String phoneNumber, String message) {
        // Mock implementation to avoid third-party API costs
        log.info("========== MOCK SMS SERVICE ==========");
        log.info("To: {}", phoneNumber);
        log.info("Message: {}", message);
        log.info("======================================");
        return true; // Always return true to simulate successful delivery
    }
}
