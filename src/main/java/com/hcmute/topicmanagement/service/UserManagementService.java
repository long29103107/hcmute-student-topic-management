package com.hcmute.topicmanagement.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserSummary> listUsers() {
        return userRepository.findAllWithRolesOrderByFullNameAsc().stream()
                .filter(user -> !hasAdminRole(user))
                .map(this::toSummary)
                .toList();
    }

    public List<RoleOption> listAssignableRoles() {
        return roleRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(RoleEntity::isSystemRole)
                .filter(role -> !isAdminRole(role))
                .map(role -> new RoleOption(role.getId(), role.getCode(), role.getName()))
                .toList();
    }

    public UserEditorData getUser(Long id) {
        UserEntity user = findUserWithRoles(id);
        Set<Long> roleIds = user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(Objects::nonNull)
                .filter(RoleEntity::isActive)
                .map(RoleEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new UserEditorData(
                user.getId(),
                user.getLoginIdentifier(),
                user.getFullName(),
                user.getEmailOrCode(),
                user.isActive(),
                roleIds);
    }

    @Transactional
    public void createUser(
            String loginIdentifier,
            String fullName,
            String emailOrCode,
            String password,
            Set<Long> roleIds) {
        String normalizedLogin = normalizeLoginIdentifier(loginIdentifier);
        validateIdentity(normalizedLogin, fullName);
        validatePassword(password, true);
        if (userRepository.existsByLoginIdentifierIgnoreCase(normalizedLogin)) {
            throw new UserValidationException("Login identifier is already in use.");
        }

        List<RoleEntity> roles = validateAndLoadRoles(roleIds);
        UserEntity user = new UserEntity(
                normalizedLogin,
                fullName.trim(),
                passwordEncoder.encode(password));
        user.setEmailOrCode(normalizeOptional(emailOrCode));
        UserEntity savedUser = userRepository.saveAndFlush(user);
        saveRoleAssignments(savedUser, roles);
    }

    @Transactional
    public void updateUser(
            Long id,
            String fullName,
            String emailOrCode,
            String newPassword,
            Set<Long> roleIds) {
        UserEntity user = findUserWithRoles(id);
        validateIdentity(user.getLoginIdentifier(), fullName);
        List<RoleEntity> roles = validateAndLoadRoles(roleIds);

        user.setFullName(fullName.trim());
        user.setEmailOrCode(normalizeOptional(emailOrCode));
        if (StringUtils.hasText(newPassword)) {
            validatePassword(newPassword, false);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
        saveRoleAssignments(user, roles);
    }

    @Transactional
    public boolean toggleActive(Long id, String currentLoginIdentifier) {
        UserEntity user = findUserWithRoles(id);
        if (user.getLoginIdentifier().equalsIgnoreCase(normalizeLoginIdentifier(currentLoginIdentifier))) {
            throw new UserValidationException("You cannot lock or unlock your own account.");
        }

        if (user.isActive() && hasActiveRole(user, "ADMIN")
                && userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("ADMIN") <= 1) {
            throw new UserValidationException("The last active administrator cannot be locked.");
        }

        user.setActive(!user.isActive());
        userRepository.save(user);
        return user.isActive();
    }

    private void saveRoleAssignments(UserEntity user, List<RoleEntity> selectedRoles) {
        Set<Long> selectedRoleIds = selectedRoles.stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserRoleEntity> existingByRole = userRoleRepository.findByUser_Id(user.getId()).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getRole().getId(),
                        assignment -> assignment,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        List<UserRoleEntity> assignmentsToSave = new ArrayList<>(existingByRole.values());

        existingByRole.values().forEach(assignment -> assignment.setActive(
                selectedRoleIds.contains(assignment.getRole().getId())));
        for (RoleEntity role : selectedRoles) {
            if (!existingByRole.containsKey(role.getId())) {
                assignmentsToSave.add(new UserRoleEntity(user, role));
            }
        }
        userRoleRepository.saveAll(assignmentsToSave);
    }

    private List<RoleEntity> validateAndLoadRoles(Set<Long> roleIds) {
        Set<Long> selectedIds = roleIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(roleIds);
        if (selectedIds.isEmpty()) {
            throw new UserValidationException("Select at least one active system role.");
        }

        List<RoleEntity> roles = roleRepository.findAllById(selectedIds);
        if (roles.size() != selectedIds.size()
                || roles.stream().anyMatch(role -> !role.isSystemRole() || !role.isActive() || isAdminRole(role))) {
            throw new UserValidationException("One or more selected roles are invalid or inactive.");
        }
        return roles;
    }

    private UserEntity findUserWithRoles(Long id) {
        if (id == null) {
            throw new UserNotFoundException(null);
        }
        UserEntity user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (hasAdminRole(user)) {
            throw new UserValidationException("Administrator accounts are managed outside this screen.");
        }
        return user;
    }

    private UserSummary toSummary(UserEntity user) {
        List<RoleBadge> roles = user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(Objects::nonNull)
                .filter(RoleEntity::isActive)
                .map(role -> new RoleBadge(role.getCode(), role.getName()))
                .toList();
        return new UserSummary(
                user.getId(),
                user.getLoginIdentifier(),
                user.getFullName(),
                user.getEmailOrCode(),
                user.isActive(),
                initials(user.getFullName(), user.getLoginIdentifier()),
                roles);
    }

    private static void validateIdentity(String loginIdentifier, String fullName) {
        if (!StringUtils.hasText(loginIdentifier)) {
            throw new UserValidationException("Login identifier is required.");
        }
        if (!StringUtils.hasText(fullName)) {
            throw new UserValidationException("Full name is required.");
        }
    }

    private static void validatePassword(String password, boolean required) {
        if (!StringUtils.hasText(password)) {
            if (required) {
                throw new UserValidationException("Password is required when creating an account.");
            }
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new UserValidationException("Password must be between 8 and 72 characters.");
        }
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(Objects::nonNull)
                .anyMatch(role -> role.isActive() && roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean hasAdminRole(UserEntity user) {
        return user.getUserRoles().stream()
                .map(UserRoleEntity::getRole)
                .filter(Objects::nonNull)
                .anyMatch(UserManagementService::isAdminRole);
    }

    private static boolean isAdminRole(RoleEntity role) {
        return "ADMIN".equalsIgnoreCase(role.getCode());
    }

    private static String normalizeLoginIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String initials(String fullName, String loginIdentifier) {
        String value = StringUtils.hasText(fullName) ? fullName.trim() : loginIdentifier;
        String[] parts = value.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    public static final class UserSummary {
        private final Long id;
        private final String loginIdentifier;
        private final String fullName;
        private final String emailOrCode;
        private final boolean active;
        private final String initials;
        private final List<RoleBadge> roles;

        public UserSummary(Long id, String loginIdentifier, String fullName, String emailOrCode,
                boolean active, String initials, List<RoleBadge> roles) {
            this.id = id;
            this.loginIdentifier = loginIdentifier;
            this.fullName = fullName;
            this.emailOrCode = emailOrCode;
            this.active = active;
            this.initials = initials;
            this.roles = List.copyOf(roles);
        }

        public Long getId() { return id; }
        public String getLoginIdentifier() { return loginIdentifier; }
        public String getFullName() { return fullName; }
        public String getEmailOrCode() { return emailOrCode; }
        public boolean isActive() { return active; }
        public String getInitials() { return initials; }
        public List<RoleBadge> getRoles() { return roles; }
    }

    public static final class UserEditorData {
        private final Long id;
        private final String loginIdentifier;
        private final String fullName;
        private final String emailOrCode;
        private final boolean active;
        private final Set<Long> roleIds;

        public UserEditorData(Long id, String loginIdentifier, String fullName, String emailOrCode,
                boolean active, Set<Long> roleIds) {
            this.id = id;
            this.loginIdentifier = loginIdentifier;
            this.fullName = fullName;
            this.emailOrCode = emailOrCode;
            this.active = active;
            this.roleIds = new LinkedHashSet<>(roleIds);
        }

        public Long getId() { return id; }
        public String getLoginIdentifier() { return loginIdentifier; }
        public String getFullName() { return fullName; }
        public String getEmailOrCode() { return emailOrCode; }
        public boolean isActive() { return active; }
        public Set<Long> getRoleIds() { return roleIds; }
    }

    public static final class RoleOption {
        private final Long id;
        private final String code;
        private final String name;

        public RoleOption(Long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
    }

    public static final class RoleBadge {
        private final String code;
        private final String name;

        public RoleBadge(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(Long id) {
            super(id == null ? "User id is required." : "User not found: " + id);
        }
    }

    public static class UserValidationException extends RuntimeException {
        public UserValidationException(String message) {
            super(message);
        }
    }
}
