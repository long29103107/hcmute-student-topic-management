package com.hcmute.topicmanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(
        name = "uk_permissions_code", columnNames = "code"))
public class PermissionEntity extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "permission_group", nullable = false, length = 80)
    private String permissionGroup;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected PermissionEntity() {
    }

    public PermissionEntity(String code, String name, String permissionGroup) {
        this.code = code;
        this.name = name;
        this.permissionGroup = permissionGroup;
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

    public String getPermissionGroup() {
        return permissionGroup;
    }

    public void setPermissionGroup(String permissionGroup) {
        this.permissionGroup = permissionGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
