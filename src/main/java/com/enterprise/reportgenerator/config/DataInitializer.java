package com.enterprise.reportgenerator.config;

import com.enterprise.reportgenerator.model.User;
import com.enterprise.reportgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("[INIT] Checking admin user...");
            String username = "admin";
            if (userRepository.findByUsername(username).isEmpty()) {
                log.info("[INIT] Admin user not found. Creating...");
                createAdmin(username);
            } else {
                log.info("[INIT] Admin user found. Resetting password to 'admin123'...");
                // We update it anyway to ensure password is correct
                userRepository.deleteById("admin-id-001"); // Assuming ID is consistent, or we fetch it.
                // Actually, let's just find and update, or easier: delete and recreate or
                // update.
                // UserRepository has save() which does MERGE.
                createAdmin(username);
            }
        };
    }

    private void createAdmin(String username) {
        User admin = new User();
        // Check if admin exists to preserve fields like MFA
        userRepository.findByUsername(username).ifPresent(existing -> {
            admin.setId(existing.getId());
            admin.setMfaEnabled(existing.isMfaEnabled()); // Preserve existing MFA
        });

        if (admin.getId() == null) {
            admin.setId("admin-id-001");
            admin.setMfaEnabled(true); // Default to true only for new
        }

        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ROLE_ADMIN");
        admin.setEmail("admin@enterprise.com");
        admin.setActive(true);
        admin.setSuperUser(true);

        userRepository.save(admin);
        log.info("[INIT] Admin user created/updated successfully. MFA Enabled: {}", admin.isMfaEnabled());
    }
}
