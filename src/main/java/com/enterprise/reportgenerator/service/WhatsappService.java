package com.enterprise.reportgenerator.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsappService {

    @Value("${twilio.account-sid:ACplaceholder}")
    private String accountSid;

    @Value("${twilio.auth-token:tokenplaceholder}")
    private String authToken;

    @Value("${twilio.whatsapp-number:whatsapp:+14155238886}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!accountSid.equals("ACplaceholder")) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized for WhatsApp MFA.");
        } else {
            log.warn("Twilio credentials not set. WhatsApp MFA will be mocked.");
        }
    }

    public void sendOtp(String to, String otp) {
        log.info("Sending OTP {} to WhatsApp: {}", otp, to);

        if (accountSid.equals("ACplaceholder")) {
            log.info("[MOCK] WhatsApp Message: Your OTP for Oracle Report Generator is: {}", otp);
            return;
        }

        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber(fromNumber),
                    "Your OTP for Oracle Report Generator is: " + otp).create();
            log.info("WhatsApp MFA sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message", e);
        }
    }
}
