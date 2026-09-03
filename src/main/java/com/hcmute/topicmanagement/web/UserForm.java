package com.hcmute.topicmanagement.web;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
