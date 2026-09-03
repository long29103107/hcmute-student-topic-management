package com.hcmute.topicmanagement.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(
        name = "uk_users_login_identifier", columnNames = "login_identifier"))
public class UserEntity extends BaseEntity {

    @Column(name = "login_identifier", nullable = false, length = 100)
    private String loginIdentifier;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email_or_code", length = 100)
    private String emailOrCode;

    /**
     * The revised schema keeps this column non-null. An empty value means the
     * administrator has not configured credentials yet; authentication rejects
     * that value before BCrypt verification.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash = "";

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserRoleEntity> userRoles = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    protected UserEntity() {
    }

    public UserEntity(String loginIdentifier, String fullName, String passwordHash) {
        this.loginIdentifier = loginIdentifier;
        this.fullName = fullName;
        setPasswordHash(passwordHash);
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash == null ? "" : passwordHash;
    }

    public Set<UserRoleEntity> getUserRoles() {
        return userRoles;
    }

    public void addRole(RoleEntity role) {
        userRoles.add(new UserRoleEntity(this, role));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
