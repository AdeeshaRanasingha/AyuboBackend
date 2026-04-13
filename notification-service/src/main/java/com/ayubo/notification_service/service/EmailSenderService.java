package com.ayubo.notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:}")
    private String fromEmail;

    public boolean sendEmail(String to, String subject, String message) {
        if (!emailEnabled || !StringUtils.hasText(to)) {
            return false;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(message);
            if (StringUtils.hasText(fromEmail)) {
                mail.setFrom(fromEmail);
            }
            mailSender.send(mail);
            return true;
        } catch (Exception ex) {
            System.out.println("Email send failed: " + ex.getMessage());
            return false;
        }
    }
}
