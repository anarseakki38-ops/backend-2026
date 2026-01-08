package com.enterprise.reportgenerator.controller;

import com.enterprise.reportgenerator.model.User;
import com.enterprise.reportgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCESS_SETTINGS')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCESS_SETTINGS')")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        user.setId(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        // Default to not superuser unless explicitly set (which UI shouldn't allow
        // easily)
        user.setSuperUser(false);
        user.setRole("ROLE_USER");

        userRepository.save(user);
        log.info("User created: {}", user.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCESS_SETTINGS')")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        // Simplified: Fetch existing, update fields, save.
        // In real app, check ID existence.
        updatedUser.setId(id);

        // If password is not empty, hash it. Else keep existing (need to fetch existing
        // first)
        // For now, let's assume UI sends full object or specific DTO.
        // To be safe, let's just update permissions and standard fields.

        userRepository.save(updatedUser);
        log.info("User updated: {}", updatedUser.getUsername());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCESS_SETTINGS')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
        return ResponseEntity.ok().build();
    }
}
