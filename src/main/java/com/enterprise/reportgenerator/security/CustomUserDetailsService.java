package com.enterprise.reportgenerator.security;

import lombok.extern.slf4j.Slf4j;
import com.enterprise.reportgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[AUTH] Loading user for security: {}", username);
        return userRepository.findByUsername(username)
                .map(user -> {
                    log.info("[AUTH] Mapping user {} to UserDetails with role: {}", username, user.getRole());

                    java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();

                    if (user.isSuperUser()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        authorities.add(new SimpleGrantedAuthority("MODIFY_DASHBOARD"));
                        authorities.add(new SimpleGrantedAuthority("MODIFY_SCHEDULER"));
                        authorities.add(new SimpleGrantedAuthority("VIEW_REPORTS"));
                        authorities.add(new SimpleGrantedAuthority("ACCESS_SETTINGS"));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        if (user.getPermissions() != null) {
                            for (String perm : user.getPermissions()) {
                                authorities.add(new SimpleGrantedAuthority(perm));
                            }
                        }
                    }

                    return org.springframework.security.core.userdetails.User.builder()
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .disabled(!user.isActive())
                            .accountExpired(false)
                            .credentialsExpired(false)
                            .accountLocked(false)
                            .authorities(authorities)
                            .build();
                })
                .orElseThrow(() -> {
                    log.error("[AUTH] UserNotFound: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }
}
