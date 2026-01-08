-- Create USERS table for Authentication & Authorization (Idempotent)
DECLARE
    v_count NUMBER;
BEGIN
    -- Check if table exists
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
        
        -- Insert Initial Admin User
        EXECUTE IMMEDIATE '
            INSERT INTO USERS (ID, USERNAME, PASSWORD, ROLE, EMAIL, MFA_ENABLED, ACTIVE)
            VALUES (''admin-id-001'', ''admin'', ''$2a$10$8.UnVuG9shgY3WvG/8D0ueLzVwYI9.t/y8jlyH1pWv2m.XU/y4y/S'', ''ROLE_ADMIN'', ''admin@enterprise.com'', ''N'', ''Y'')
        ';
        
        COMMIT;
    END IF;
END;
/

