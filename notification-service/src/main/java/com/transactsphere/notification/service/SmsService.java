package com.transactsphere.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${spring.twilio.account-sid:}")
    private String accountSid;

    @Value("${spring.twilio.auth-token:}")
    private String authToken;

    @Value("${spring.twilio.phone-number:}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SDK initialized successfully.");
        } else {
            log.warn("Twilio credentials are not set. SMS sending will fail.");
        }
    }

    public boolean sendSms(String phoneNumber, String message) {
        if (accountSid == null || accountSid.isBlank()) {
            log.warn("Cannot send SMS: Twilio Account SID is missing.");
            return false;
        }

        try {
            Message sms = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    message
            ).create();
            log.info("SMS sent successfully to {}. SID: {}", phoneNumber, sms.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
