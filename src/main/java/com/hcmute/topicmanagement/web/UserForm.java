package com.hcmute.topicmanagement.web;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UserForm {

    @Size(max = 100, message = "Login identifier must be 100 characters or fewer.")
    private String loginIdentifier;

    private String accountType;

    @NotBlank(message = "Full name is required.")
    @Size(max = 150, message = "Full name must be 150 characters or fewer.")
    private String fullName;

    @Size(max = 100, message = "Email must be 100 characters or fewer.")
    private String emailOrCode;

    @Pattern(regexp = "^$|[0-9]{8}", message = "Student code must contain exactly 8 digits.")
    private String studentCode;

    @Size(max = 20, message = "Academic year must be 20 characters or fewer.")
    private String academicYear;

    @Size(max = 100, message = "Major must be 100 characters or fewer.")
    private String major;

    @Size(max = 100, message = "Class must be 100 characters or fewer.")
    private String className;

    @Size(max = 30, message = "Phone must be 30 characters or fewer.")
    private String phone;

    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    @Size(max = 72, message = "Password must be 72 characters or fewer.")
    private String password;

    private Set<Long> roleIds = new LinkedHashSet<>();

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmailOrCode() {
        return emailOrCode;
    }

    public void setEmailOrCode(String emailOrCode) {
        this.emailOrCode = emailOrCode;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roleIds);
    }
}
