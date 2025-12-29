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

    @Value("${app.email.mock:false}")
    private boolean mockEmail;

    public void sendReportEmail(List<String> recipients, String subject, String body, File attachment) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("No recipients defined for email.");
            return;
        }

        if (mockEmail) {
            log.info("============== MOCK EMAIL SENT ==============");
            log.info("To: {}", recipients);
            log.info("Subject: {}", subject);
            log.info("Body: {}", body);
            if (attachment != null) {
                log.info("Attachment: {}", attachment.getName());
            }
            log.info("============================================");
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
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("Email Send Failed: " + e.getMessage(), e);
        }
    }
}
