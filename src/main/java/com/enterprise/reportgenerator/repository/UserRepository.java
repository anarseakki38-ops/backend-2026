package com.enterprise.reportgenerator.repository;

import com.enterprise.reportgenerator.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getString("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setPassword(rs.getString("PASSWORD"));
        user.setRole(rs.getString("ROLE"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPhoneNumber(rs.getString("PHONE_NUMBER"));
        user.setMfaEnabled("Y".equals(rs.getString("MFA_ENABLED")));
        user.setActive("Y".equals(rs.getString("ACTIVE")));
        user.setSuperUser("Y".equals(rs.getString("IS_SUPERUSER")));
        return user;
    };

    public Optional<User> findByUsername(String identifier) {
        log.info("[DB] Searching for user by username or email: {}", identifier);
        // Allow login by Username OR Email
        String sql = "SELECT * FROM USERS WHERE USERNAME = ? OR EMAIL = ?";
        try {
            // We pass 'identifier' twice, once for username check, once for email check
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, identifier, identifier);
            loadPermissions(user);
            log.info("[DB] User found: {} with ID: {}", user.getUsername(), user.getId());
            return Optional.ofNullable(user);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("[DB] User NOT found: {}", identifier);
            return Optional.empty();
        } catch (Exception e) {
            log.error("[DB] Error searching for user: {}. Message: {}", identifier, e.getMessage());
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        List<User> users = jdbcTemplate.query("SELECT * FROM USERS", userRowMapper);
        users.forEach(this::loadPermissions);
        return users;
    }

    private void loadPermissions(User user) {
        if (user == null)
            return;
        String sql = "SELECT PERMISSION_NAME FROM USER_PERMISSIONS WHERE USER_ID = ?";
        List<String> perms = jdbcTemplate.queryForList(sql, String.class, user.getId());
        user.setPermissions(new java.util.HashSet<>(perms));
    }

    public void save(User user) {
        // 1. Save User Base Data
        String sql = "MERGE INTO USERS dst USING (SELECT ? id, ? uname, ? pwd, ? role, ? email, ? phone, ? mfa, ? active, ? superuser FROM dual) src "
                +
                "ON (dst.USERNAME = src.uname) " +
                "WHEN MATCHED THEN UPDATE SET dst.PASSWORD = src.pwd, dst.ROLE = src.role, dst.EMAIL = src.email, " +
                "dst.PHONE_NUMBER = src.phone, dst.MFA_ENABLED = src.mfa, dst.ACTIVE = src.active, dst.IS_SUPERUSER = src.superuser "
                +
                "WHEN NOT MATCHED THEN INSERT (ID, USERNAME, PASSWORD, ROLE, EMAIL, PHONE_NUMBER, MFA_ENABLED, ACTIVE, IS_SUPERUSER) "
                +
                "VALUES (src.id, src.uname, src.pwd, src.role, src.email, src.phone, src.mfa, src.active, src.superuser)";

        jdbcTemplate.update(sql,
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.isMfaEnabled() ? "Y" : "N",
                user.isActive() ? "Y" : "N",
                user.isSuperUser() ? "Y" : "N");

        // 2. Save Permissions (Delete & Re-insert strategy for simplicity)
        if (user.getPermissions() != null) {
            jdbcTemplate.update("DELETE FROM USER_PERMISSIONS WHERE USER_ID = ?", user.getId());
            for (String perm : user.getPermissions()) {
                jdbcTemplate.update("INSERT INTO USER_PERMISSIONS (USER_ID, PERMISSION_NAME) VALUES (?, ?)",
                        user.getId(), perm);
            }
        }
    }

    public void toggleMfa(String username, boolean enabled) {
        String sql = "UPDATE USERS SET MFA_ENABLED = ? WHERE USERNAME = ?";
        jdbcTemplate.update(sql, enabled ? "Y" : "N", username);
    }

    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM USERS WHERE ID = ?", id);
    }
}
