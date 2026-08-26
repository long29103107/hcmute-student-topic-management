-- HCMUTE Student Topic Management
-- MySQL 8+ / InnoDB / utf8mb4
-- DDL only: creates the database and MVP schema. No DROP and no seed data.
-- Run from the project root:
--   mysql -u root -p < database/ddl.sql
-- DBeaver: use "Execute SQL Script" (Alt+X), not "Execute SQL Statement"
-- (Ctrl+Enter), because this file contains multiple MySQL statements.

CREATE DATABASE IF NOT EXISTS hcmute_topic_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hcmute_topic_management;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ================================================================
-- Authentication and authorization
-- ================================================================

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(150) NOT NULL,
    permission_group VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_identifier VARCHAR(100) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email_or_code VARCHAR(100) NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_login_identifier UNIQUE (login_identifier)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id),
    INDEX idx_user_roles_user_active (user_id, active),
    INDEX idx_user_roles_role_active (role_id, active)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_role_permissions PRIMARY KEY (id),
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
    INDEX idx_role_permissions_role_active (role_id, active),
    INDEX idx_role_permissions_permission_active (permission_id, active)
) ENGINE = InnoDB;

-- ================================================================
-- Core MVP domain
-- ================================================================

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS registration_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL,
    lecturer_registration_start DATETIME(6) NOT NULL,
    lecturer_registration_end DATETIME(6) NOT NULL,
    student_registration_start DATETIME(6) NOT NULL,
    student_registration_end DATETIME(6) NOT NULL,
    reviewer_score_deadline DATETIME(6) NULL,
    council_report_date DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_registration_periods PRIMARY KEY (id),
    CONSTRAINT fk_registration_periods_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_registration_periods_status (status),
    INDEX idx_registration_periods_student_window
        (student_registration_start, student_registration_end),
    INDEX idx_registration_periods_lecturer_window
        (lecturer_registration_start, lecturer_registration_end)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    period_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    proposed_by BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_topics PRIMARY KEY (id),
    CONSTRAINT fk_topics_period FOREIGN KEY (period_id) REFERENCES registration_periods (id),
    CONSTRAINT fk_topics_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_topics_proposed_by FOREIGN KEY (proposed_by) REFERENCES users (id),
    INDEX idx_topics_period_status (period_id, status),
    INDEX idx_topics_department_status (department_id, status),
    INDEX idx_topics_proposed_by (proposed_by)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS topic_supervisors (
    topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    CONSTRAINT pk_topic_supervisors PRIMARY KEY (topic_id, lecturer_id),
    CONSTRAINT fk_topic_supervisors_topic FOREIGN KEY (topic_id) REFERENCES topics (id),
    CONSTRAINT fk_topic_supervisors_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id),
    INDEX idx_topic_supervisors_lecturer (lecturer_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS student_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    created_by BIGINT NOT NULL,
    leader_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_student_groups PRIMARY KEY (id),
    CONSTRAINT fk_student_groups_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_student_groups_leader FOREIGN KEY (leader_id) REFERENCES users (id),
    INDEX idx_student_groups_status (status),
    INDEX idx_student_groups_leader (leader_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS group_members (
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT pk_group_members PRIMARY KEY (group_id, student_id),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES student_groups (id),
    CONSTRAINT fk_group_members_student FOREIGN KEY (student_id) REFERENCES users (id),
    INDEX idx_group_members_student (student_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS topic_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_topic_registrations PRIMARY KEY (id),
    CONSTRAINT uk_registration_group_period UNIQUE (group_id, period_id),
    CONSTRAINT fk_topic_registrations_group FOREIGN KEY (group_id) REFERENCES student_groups (id),
    CONSTRAINT fk_topic_registrations_topic FOREIGN KEY (topic_id) REFERENCES topics (id),
    CONSTRAINT fk_topic_registrations_period FOREIGN KEY (period_id) REFERENCES registration_periods (id),
    CONSTRAINT fk_topic_registrations_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    INDEX idx_topic_registrations_status (status),
    INDEX idx_topic_registrations_topic (topic_id),
    INDEX idx_topic_registrations_group (group_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NULL,
    file_size BIGINT NULL,
    uploader_id BIGINT NOT NULL,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reports PRIMARY KEY (id),
    CONSTRAINT fk_reports_registration FOREIGN KEY (registration_id)
        REFERENCES topic_registrations (id),
    CONSTRAINT fk_reports_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    INDEX idx_reports_registration_submitted (registration_id, submitted_at),
    INDEX idx_reports_uploader (uploader_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    score DECIMAL(5, 2) NULL,
    average_score DECIMAL(5, 2) NULL,
    comment TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_evaluations PRIMARY KEY (id),
    CONSTRAINT uk_evaluations_registration UNIQUE (registration_id),
    CONSTRAINT fk_evaluations_registration FOREIGN KEY (registration_id)
        REFERENCES topic_registrations (id),
    CONSTRAINT fk_evaluations_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id),
    INDEX idx_evaluations_lecturer_status (lecturer_id, status)
) ENGINE = InnoDB;
