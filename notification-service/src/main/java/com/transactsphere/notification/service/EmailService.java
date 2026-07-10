package com.transactsphere.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public boolean sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            helper.setFrom("no-reply@transactsphere.com", "TransactSphere");
            mailSender.send(message);

            // admin copy removed

            log.info("Email sent successfully to {} and admin@transactsphere.com", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            return false;
        }
    }
}
