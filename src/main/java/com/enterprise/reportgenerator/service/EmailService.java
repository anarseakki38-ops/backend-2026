package com.enterprise.reportgenerator.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:sender@example.com}")
    private String senderEmail;

    public void sendReportEmail(List<String> recipients, String subject, String body, File attachment) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("No recipients defined for email.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body);

            if (attachment != null && attachment.exists()) {
                helper.addAttachment(attachment.getName(), attachment);
            }

            mailSender.send(message);
            log.info("Email sent to {}", recipients);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }
}
