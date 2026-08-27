package com.hcmute.topicmanagement.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.PermissionEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.RolePermissionEntity;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;

@Service
@Transactional(readOnly = true)
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleManagementService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<RoleSummary> listRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .filter(RoleEntity::isSystemRole)
                .filter(role -> !isAdminRole(role))
                .map(role -> new RoleSummary(
                        role.getId(),
                        role.getCode(),
                        role.getName(),
                        role.getDescription(),
                        role.isSystemRole(),
                        role.isActive(),
                        rolePermissionRepository.countByRole_IdAndActiveTrue(role.getId())))
                .toList();
    }

    public RolePermissionData getRoleForPermissions(Long id) {
        RoleEntity role = findRoleWithPermissions(id);
        rejectHiddenOrNonSystemRole(role);
        Set<Long> selectedPermissionIds = role.getRolePermissions().stream()
                .filter(RolePermissionEntity::isActive)
                .map(RolePermissionEntity::getPermission)
                .filter(Objects::nonNull)
                .filter(PermissionEntity::isActive)
                .map(PermissionEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new RolePermissionData(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                role.isActive(),
                selectedPermissionIds);
    }

    public List<PermissionGroupView> getPermissionGroups() {
        Map<String, List<PermissionView>> groupedPermissions = new LinkedHashMap<>();
        permissionRepository.findByActiveTrueOrderByPermissionGroupAscNameAsc()
                .stream()
                .filter(permission -> !"ADMIN".equalsIgnoreCase(permission.getCode()))
                .forEach(permission -> groupedPermissions
                        .computeIfAbsent(permission.getPermissionGroup(), ignored -> new ArrayList<>())
                        .add(new PermissionView(
                                permission.getId(),
                                permission.getCode(),
                                permission.getName(),
                                permission.getDescription())));

        return groupedPermissions.entrySet().stream()
                .map(entry -> new PermissionGroupView(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public void updateRolePermissions(Long id, Set<Long> permissionIds) {
        RoleEntity role = findRoleWithPermissions(id);
        rejectHiddenOrNonSystemRole(role);
        syncPermissions(role, permissionIds);
    }

    private void syncPermissions(RoleEntity role, Set<Long> requestedPermissionIds) {
        Set<Long> selectedIds = requestedPermissionIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(requestedPermissionIds);
        List<PermissionEntity> selectedPermissions = selectedIds.isEmpty()
                ? List.of()
                : permissionRepository.findAllById(selectedIds);

        if (selectedPermissions.size() != selectedIds.size()
                || selectedPermissions.stream().anyMatch(permission -> !permission.isActive())) {
            throw new RoleValidationException("One or more selected permissions are invalid or inactive.");
        }

        Map<Long, RolePermissionEntity> existingByPermission = rolePermissionRepository
                .findByRole_Id(role.getId()).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getPermission().getId(),
                        assignment -> assignment,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        List<RolePermissionEntity> assignmentsToSave = new ArrayList<>(existingByPermission.values());

        existingByPermission.values().forEach(assignment -> assignment.setActive(
                selectedIds.contains(assignment.getPermission().getId())));
        for (PermissionEntity permission : selectedPermissions) {
            if (!existingByPermission.containsKey(permission.getId())) {
                assignmentsToSave.add(new RolePermissionEntity(role, permission));
            }
        }
        rolePermissionRepository.saveAll(assignmentsToSave);
    }

    private RoleEntity findRoleWithPermissions(Long id) {
        if (id == null) {
            throw new RoleNotFoundException(null);
        }
        return roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }

    private static void rejectHiddenOrNonSystemRole(RoleEntity role) {
        if (!role.isSystemRole()) {
            throw new RoleValidationException("Only system roles can be managed.");
        }
        if (isAdminRole(role)) {
            throw new RoleValidationException("The ADMIN system role is managed outside this screen.");
        }
    }

    private static boolean isAdminRole(RoleEntity role) {
        return "ADMIN".equalsIgnoreCase(role.getCode());
    }

    public static final class RoleSummary {
        private final Long id;
        private final String code;
        private final String name;
        private final String description;
        private final boolean systemRole;
        private final boolean active;
        private final long permissionCount;

        public RoleSummary(Long id, String code, String name, String description,
                boolean systemRole, boolean active, long permissionCount) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
            this.systemRole = systemRole;
            this.active = active;
            this.permissionCount = permissionCount;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isSystemRole() { return systemRole; }
        public boolean isActive() { return active; }
        public long getPermissionCount() { return permissionCount; }
    }

    public static final class RolePermissionData {
        private final Long id;
        private final String code;
        private final String name;
        private final String description;
        private final boolean systemRole;
        private final boolean active;
        private final Set<Long> permissionIds;

        public RolePermissionData(Long id, String code, String name, String description,
                boolean systemRole, boolean active, Set<Long> permissionIds) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
            this.systemRole = systemRole;
            this.active = active;
            this.permissionIds = new LinkedHashSet<>(permissionIds);
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isSystemRole() { return systemRole; }
        public boolean isActive() { return active; }
        public Set<Long> getPermissionIds() { return permissionIds; }
    }

    public static final class PermissionGroupView {
        private final String name;
        private final List<PermissionView> permissions;

        public PermissionGroupView(String name, List<PermissionView> permissions) {
            this.name = name;
            this.permissions = List.copyOf(permissions);
        }

        public String getName() { return name; }
        public List<PermissionView> getPermissions() { return permissions; }
    }

    public static final class PermissionView {
        private final Long id;
        private final String code;
        private final String name;
        private final String description;

        public PermissionView(Long id, String code, String name, String description) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(Long id) {
            super(id == null ? "Role id is required." : "Role not found: " + id);
        }
    }

    public static class RoleValidationException extends RuntimeException {
        public RoleValidationException(String message) {
            super(message);
        }
    }
}
