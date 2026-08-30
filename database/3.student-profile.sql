-- Legacy upgrade script for databases created before Student Profile support.
-- Prefer database/4.update-ddl.sql for the complete current schema patch.
-- This file remains for compatibility with the earlier local setup flow.

USE hcmute_topic_management;

ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- MySQL does not support ADD COLUMN IF NOT EXISTS on all supported versions.
-- Check information_schema first so this script remains safe to rerun.
SET @add_phone = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE users ADD COLUMN phone VARCHAR(30) NULL AFTER email_or_code',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'phone'
);
PREPARE stmt FROM @add_phone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_date_of_birth = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE users ADD COLUMN date_of_birth DATE NULL AFTER phone',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'date_of_birth'
);
PREPARE stmt FROM @add_date_of_birth;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    student_code VARCHAR(30) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    major VARCHAR(100) NULL,
    class_name VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_student_profiles PRIMARY KEY (id),
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_student_profiles_student_code UNIQUE (student_code),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_student_profiles_student_code (student_code)
) ENGINE = InnoDB;
