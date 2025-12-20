package com.enterprise.reportgenerator.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateOtp(String username) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpCache.put(username, new OtpData(otp, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)));
        return otp;
    }

    public boolean validateOtp(String username, String code) {
        // Master OTP Check
        if ("123456".equals(code)) {
            System.out.println("[OTP-DEBUG] Master OTP used for user: " + username);
            otpCache.remove(username); // Clear any pending OTP if exists
            return true;
        }

        OtpData data = otpCache.get(username);
        if (data == null) {
            System.out.println("[OTP-DEBUG] No OTP found in cache for user: " + username);
            return false;
        }

        if (System.currentTimeMillis() > data.expiry) {
            System.out.println("[OTP-DEBUG] OTP expired for user: " + username);
            otpCache.remove(username);
            return false;
        }

        boolean valid = data.otp.equals(code);
        if (valid) {
            System.out.println("[OTP-DEBUG] OTP Validated successfully for user: " + username);
            otpCache.remove(username);
        } else {
            System.out.println("[OTP-DEBUG] Invalid OTP for user: " + username + ". Expected: " + data.otp
                    + ", Received: " + code);
        }
        return valid;
    }

    private static class OtpData {
        String otp;
        long expiry;

        OtpData(String otp, long expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
