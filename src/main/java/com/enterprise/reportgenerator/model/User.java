package com.enterprise.reportgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String password;
    private String role; // ROLE_ADMIN, ROLE_USER
    private String email;
    private String phoneNumber;
    private boolean mfaEnabled = false;
    private boolean active = true;

    // RBAC Fields
    private boolean superUser = false;
    private java.util.Set<String> permissions; // MODIFY_DASHBOARD, ACCESS_SETTINGS, etc.
}
