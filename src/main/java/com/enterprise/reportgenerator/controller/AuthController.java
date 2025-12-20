package com.enterprise.reportgenerator.controller;

import com.enterprise.reportgenerator.model.User;
import com.enterprise.reportgenerator.repository.UserRepository;
import com.enterprise.reportgenerator.security.JwtService;
import com.enterprise.reportgenerator.service.OtpService;
import com.enterprise.reportgenerator.service.WhatsappService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private final WhatsappService whatsappService;
    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("[AUTH] Login attempt for user: {}", request.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            log.info("[AUTH] Password authentication successful for user: {}", request.getUsername());
        } catch (Exception e) {
            log.warn("[AUTH] Authentication failed for user: {}. Error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User data not found for: " + request.getUsername()));

        log.info("[AUTH] User found in DB: {}, MFA Enabled: {}", user.getUsername(), user.isMfaEnabled());

        if (user.isMfaEnabled()) {
            String otp = otpService.generateOtp(user.getUsername());
            if (user.getPhoneNumber() != null) {
                whatsappService.sendOtp(user.getPhoneNumber(), otp);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mfaRequired", true);
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(createAuthResponse(userDetails, user));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
        if (otpService.validateOtp(request.getUsername(), request.getOtp())) {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            final User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
            return ResponseEntity.ok(createAuthResponse(userDetails, user));
        }
        return ResponseEntity.status(401).body("Invalid or expired OTP");
    }

    @PatchMapping("/mfa/toggle")
    public ResponseEntity<?> toggleMfa(@RequestBody Map<String, Boolean> request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean enabled = request.getOrDefault("enabled", false);
        userRepository.toggleMfa(username, enabled);
        return ResponseEntity.ok(Map.of("message", "MFA " + (enabled ? "enabled" : "disabled") + " successfully"));
    }

    private Map<String, Object> createAuthResponse(UserDetails userDetails, User user) {
        final String jwt = jwtService.generateToken(userDetails);
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("superUser", user.isSuperUser());
        response.put("permissions", user.getPermissions());
        return response;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class OtpRequest {
        private String username;
        private String otp;
    }
}
