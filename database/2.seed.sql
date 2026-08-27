-- HCMUTE Student Topic Management
-- Seed data for local development only. Run database/1.ddl.sql first.
-- No plaintext password is stored here; password_hash is BCrypt.

USE hcmute_topic_management;

-- Every role is system-managed. Role profiles cannot be created, edited, or deleted from the UI.
UPDATE roles
SET system_role = TRUE;

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
    'Manage system accounts, roles, and permissions.',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- ================================================================
-- Standard system roles used by the application
-- ================================================================

INSERT INTO roles (
    code,
    name,
    description,
    system_role,
    active,
    created_at,
    updated_at
) VALUES
    (
        'FACULTY_HEAD',
        'Faculty Head',
        'Manage faculty departments, registration periods, topic reviews, and results.',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        'LECTURER',
        'Lecturer',
        'Propose topics, supervise students, and submit evaluations.',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        'STUDENT',
        'Student',
        'Create groups, register topics, submit reports, and view group results.',
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    )
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    system_role = TRUE,
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
    ('DASHBOARD_VIEW', 'View dashboard', 'Dashboard', 'View the administration dashboard.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_READ', 'View accounts', 'Users', 'View the account list and account details.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_CREATE', 'Create account', 'Users', 'Create a new user account.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_UPDATE', 'Update account', 'Users', 'Update account information.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_LOCK', 'Lock/unlock account', 'Users', 'Lock or unlock an account.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('USER_ROLE_ASSIGN', 'Assign role to user', 'Users', 'Assign or remove a user role.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_READ', 'View roles', 'Roles', 'View the role list and current permissions.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_UPDATE', 'Manage role permissions', 'Roles', 'Open the role permission assignment screen.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PERMISSION_ASSIGN', 'Assign permissions to role', 'Roles', 'Enable or disable permissions for a role.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('DEPARTMENT_MANAGE', 'Manage departments', 'Departments', 'Create and update faculty departments.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('PERIOD_MANAGE', 'Manage registration periods', 'Registration periods', 'Create and manage topic registration periods.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('TOPIC_PROPOSE', 'Propose topics', 'Topics', 'Create and update topic proposals.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('TOPIC_REVIEW', 'Review topics', 'Topics', 'Review, approve, reject, and publish topic proposals.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('TOPIC_VIEW', 'View published topics', 'Topics', 'View topics published for registration.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('GROUP_MANAGE', 'Manage student groups', 'Student groups', 'Create and manage student group membership.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REGISTRATION_SUBMIT', 'Submit topic registrations', 'Registrations', 'Submit a topic registration for a student group.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('REPORT_SUBMIT', 'Submit reports', 'Reports', 'Submit reports for an approved topic registration.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('EVALUATION_SUBMIT', 'Submit evaluations', 'Evaluations', 'Submit evaluation scores and comments.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('RESULT_VIEW', 'View results', 'Results', 'View published results for permitted users.', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission_group = VALUES(permission_group),
    description = VALUES(description),
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- Role profiles are fixed system data; disable the legacy role-creation permission if it exists.
UPDATE permissions
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'ROLE_CREATE';

-- ================================================================
-- Sample permissions for the standard system roles
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
WHERE r.code = 'FACULTY_HEAD'
  AND p.code IN (
      'DEPARTMENT_MANAGE',
      'PERIOD_MANAGE',
      'TOPIC_REVIEW',
      'TOPIC_VIEW',
      'RESULT_VIEW'
  )
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

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
WHERE r.code = 'LECTURER'
  AND p.code IN (
      'TOPIC_PROPOSE',
      'TOPIC_VIEW',
      'EVALUATION_SUBMIT',
      'RESULT_VIEW'
  )
ON DUPLICATE KEY UPDATE
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

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
WHERE r.code = 'STUDENT'
  AND p.code IN (
      'TOPIC_VIEW',
      'GROUP_MANAGE',
      'REGISTRATION_SUBMIT',
      'REPORT_SUBMIT',
      'RESULT_VIEW'
  )
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

SELECT code, name, system_role, active
FROM roles
WHERE code IN ('FACULTY_HEAD', 'LECTURER', 'STUDENT')
ORDER BY code;
