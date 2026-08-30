package com.hcmute.topicmanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_profiles", uniqueConstraints = @UniqueConstraint(
        name = "uk_student_profiles_student_code", columnNames = "student_code"))
public class StudentProfileEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "student_code", nullable = false, length = 30)
    private String studentCode;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(length = 100)
    private String major;

    @Column(name = "class_name", length = 100)
    private String className;

    protected StudentProfileEntity() {
    }

    public StudentProfileEntity(UserEntity user, String studentCode, String academicYear,
            String major, String className) {
        this.user = user;
        this.studentCode = studentCode;
        this.academicYear = academicYear;
        this.major = major;
        this.className = className;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
