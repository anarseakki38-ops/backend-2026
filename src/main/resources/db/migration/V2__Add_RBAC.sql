-- Ensure USERS table exists (in case V1 was baselined and never ran)
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'USERS';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE USERS (
                ID VARCHAR2(50) PRIMARY KEY,
                USERNAME VARCHAR2(50) UNIQUE NOT NULL,
                PASSWORD VARCHAR2(255) NOT NULL,
                ROLE VARCHAR2(20) DEFAULT ''ROLE_USER'' NOT NULL,
                EMAIL VARCHAR2(100),
                PHONE_NUMBER VARCHAR2(20),
                MFA_ENABLED CHAR(1) DEFAULT ''N'' CHECK (MFA_ENABLED IN (''Y'', ''N'')),
                ACTIVE CHAR(1) DEFAULT ''Y'' CHECK (ACTIVE IN (''Y'', ''N'')),
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ';
        
        EXECUTE IMMEDIATE '
            INSERT INTO USERS (ID, USERNAME, PASSWORD, ROLE, EMAIL, MFA_ENABLED, ACTIVE)
            VALUES (''admin-id-001'', ''admin'', ''$2a$10$8.UnVuG9shgY3WvG/8D0ueLzVwYI9.t/y8jlyH1pWv2m.XU/y4y/S'', ''ROLE_ADMIN'', ''admin@enterprise.com'', ''N'', ''Y'')
        ';
        
        COMMIT;
    END IF;
END;
/

-- Add IS_SUPERUSER column to USERS table (Idempotent)
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count 
    FROM user_tab_columns 
    WHERE table_name = 'USERS' AND column_name = 'IS_SUPERUSER';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USERS ADD (IS_SUPERUSER CHAR(1) DEFAULT ''N'' NOT NULL)';
    END IF;
END;
/

-- Create USER_PERMISSIONS table (Idempotent)
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'USER_PERMISSIONS';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE USER_PERMISSIONS (
                USER_ID VARCHAR2(50) NOT NULL,
                PERMISSION_NAME VARCHAR2(50) NOT NULL,
                CONSTRAINT fk_user_perm FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE
            )
        ';
        
        EXECUTE IMMEDIATE 'CREATE INDEX idx_user_perm_uid ON USER_PERMISSIONS(USER_ID)';
    END IF;
END;
/

-- Set admin as Super User (Idempotent)
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USERS WHERE USERNAME = 'admin' AND IS_SUPERUSER = 'Y';
    
    IF v_count = 0 THEN
        UPDATE USERS SET IS_SUPERUSER = 'Y' WHERE USERNAME = 'admin';
        COMMIT;
    END IF;
END;
/

