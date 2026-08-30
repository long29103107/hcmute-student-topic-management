-- HCMUTE Student Topic Management
-- Idempotent schema patch for databases created before the current User and
-- Student Profile model. Run this file against the existing MySQL database.
-- It fixes the runtime error caused by missing users.date_of_birth and adds
-- the Student Profile table/constraints required by the application.

CREATE DATABASE IF NOT EXISTS hcmute_topic_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hcmute_topic_management;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NULL;

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

SET @add_profile_academic_year = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE student_profiles ADD COLUMN academic_year VARCHAR(20) NOT NULL DEFAULT ''unknown'' AFTER student_code',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'student_profiles'
      AND column_name = 'academic_year'
);
PREPARE stmt FROM @add_profile_academic_year;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_profile_major = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE student_profiles ADD COLUMN major VARCHAR(100) NULL AFTER academic_year',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'student_profiles'
      AND column_name = 'major'
);
PREPARE stmt FROM @add_profile_major;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_profile_class_name = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE student_profiles ADD COLUMN class_name VARCHAR(100) NULL AFTER major',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'student_profiles'
      AND column_name = 'class_name'
);
PREPARE stmt FROM @add_profile_class_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_student_code_unique = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE student_profiles ADD CONSTRAINT uk_student_profiles_student_code UNIQUE (student_code)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'student_profiles'
      AND index_name = 'uk_student_profiles_student_code'
);
PREPARE stmt FROM @add_student_code_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_user_unique = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE student_profiles ADD CONSTRAINT uk_student_profiles_user UNIQUE (user_id)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'student_profiles'
      AND index_name = 'uk_student_profiles_user'
);
PREPARE stmt FROM @add_user_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
