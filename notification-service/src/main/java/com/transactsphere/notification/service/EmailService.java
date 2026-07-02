package com.transactsphere.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("no-reply@transactsphere.com");
            mailSender.send(message);

            // Send a completely separate email to the official mail ID
            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setTo("admin@transactsphere.com");
            adminMessage.setSubject("[COPY] " + subject);
            adminMessage.setText("This is a copy of a notification sent to " + to + "\n\n" + text);
            adminMessage.setFrom("no-reply@transactsphere.com");
            mailSender.send(adminMessage);

            log.info("Email sent successfully to {} and admin@transactsphere.com", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            return false;
        }
    }
}
