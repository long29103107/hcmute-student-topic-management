-- HCMUTE Student Topic Management
-- Seed data for local development only. Run database/ddl.sql first.
-- No plaintext password is stored here; password_hash is BCrypt.

USE hcmute_topic_management;

-- ================================================================
-- Admin role
-- ================================================================

INSERT INTO roles (
    code,
    name,
    description,
    system_role,
    active,
    created_at,
    updated_at
) VALUES (
    'ADMIN',
    'Administrator',
    'Quản trị tài khoản, role và permission của hệ thống.',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Admin permissions
-- ================================================================

INSERT INTO permissions (
    code,
    name,
    permission_group,
    description,
    active,
    created_at,
    updated_at
) VALUES
    ('DASHBOARD_VIEW', 'Xem dashboard', 'Dashboard', 'Xem dashboard quản trị.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_READ', 'Xem tài khoản', 'Users', 'Xem danh sách và thông tin tài khoản.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_CREATE', 'Tạo tài khoản', 'Users', 'Tạo tài khoản người dùng mới.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_UPDATE', 'Cập nhật tài khoản', 'Users', 'Cập nhật thông tin tài khoản.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_LOCK', 'Khóa/mở khóa tài khoản', 'Users', 'Khóa hoặc mở khóa tài khoản.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_ROLE_ASSIGN', 'Gán role cho user', 'Users', 'Gán hoặc bỏ role của người dùng.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_READ', 'Xem role', 'Roles', 'Xem danh sách role và permission hiện tại.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_CREATE', 'Tạo role', 'Roles', 'Tạo role mới.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_UPDATE', 'Cập nhật role', 'Roles', 'Cập nhật thông tin role.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PERMISSION_ASSIGN', 'Gán permission cho role', 'Roles', 'Bật hoặc tắt permission trên role.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Local admin account
-- Login: admin
-- Change this password before sharing or deploying the application.
-- ================================================================

INSERT INTO users (
    login_identifier,
    full_name,
    email_or_code,
    password_hash,
    active,
    created_at,
    updated_at
) VALUES (
    'admin',
    'System Administrator',
    'admin@hcmute.local',
    '$2a$10$6iC74PQoleTF9Yy2Cjkw0OX7EOD2QuRlOxqbBrdlyooMSItw6WbiK',
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Assign all admin permissions to ADMIN
-- ================================================================

INSERT INTO role_permissions (
    role_id,
    permission_id,
    assigned_at,
    active,
    created_at,
    updated_at
)
SELECT
    r.id,
    p.id,
    CURRENT_TIMESTAMP(6),
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM roles r
JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'DASHBOARD_VIEW',
      'USER_READ',
      'USER_CREATE',
      'USER_UPDATE',
      'USER_LOCK',
      'USER_ROLE_ASSIGN',
      'ROLE_READ',
      'ROLE_CREATE',
      'ROLE_UPDATE',
      'PERMISSION_ASSIGN'
  )
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Assign ADMIN role to the local admin account
-- ================================================================

INSERT INTO user_roles (
    user_id,
    role_id,
    assigned_at,
    active,
    created_at,
    updated_at
)
SELECT
    u.id,
    r.id,
    CURRENT_TIMESTAMP(6),
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM users u
JOIN roles r
WHERE u.login_identifier = 'admin'
  AND r.code = 'ADMIN'
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Verification
-- ================================================================

SELECT u.login_identifier, r.code AS role_code, COUNT(rp.id) AS permission_count
FROM users u
JOIN user_roles ur ON ur.user_id = u.id AND ur.active = TRUE
JOIN roles r ON r.id = ur.role_id AND r.active = TRUE
LEFT JOIN role_permissions rp ON rp.role_id = r.id AND rp.active = TRUE
WHERE u.login_identifier = 'admin'
GROUP BY u.login_identifier, r.code;
