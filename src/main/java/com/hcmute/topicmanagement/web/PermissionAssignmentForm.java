package com.hcmute.topicmanagement.web;

import java.util.LinkedHashSet;
import java.util.Set;

public class PermissionAssignmentForm {

    private Set<Long> permissionIds = new LinkedHashSet<>();

    public Set<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(Set<Long> permissionIds) {
        this.permissionIds = permissionIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(permissionIds);
    }
}
