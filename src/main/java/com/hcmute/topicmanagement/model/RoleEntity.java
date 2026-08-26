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
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(
        name = "uk_roles_code", columnNames = "code"))
public class RoleEntity extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole = true;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<UserRoleEntity> userRoles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<RolePermissionEntity> rolePermissions = new LinkedHashSet<>();

    protected RoleEntity() {
    }

    public RoleEntity(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean systemRole) {
        this.systemRole = systemRole;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<UserRoleEntity> getUserRoles() {
        return userRoles;
    }

    public Set<RolePermissionEntity> getRolePermissions() {
        return rolePermissions;
    }

    public void addUser(UserEntity user) {
        userRoles.add(new UserRoleEntity(user, this));
    }

    public void addPermission(PermissionEntity permission) {
        rolePermissions.add(new RolePermissionEntity(this, permission));
    }

    public void removePermission(PermissionEntity permission) {
        rolePermissions.removeIf(rolePermission ->
                rolePermission.getPermission().getId().equals(permission.getId()));
    }
}
