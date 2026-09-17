package com.gupta.linkly.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@linkly.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Linkly - Password Reset Request");
            message.setText("Hello,\n\nYou have requested to reset your password.\n" +
                    "Click the link below to change your password:\n\n" +
                    resetLink + "\n\n" +
                    "This link will expire in 30 minutes.\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Thanks,\nThe Linkly Team");
            
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
