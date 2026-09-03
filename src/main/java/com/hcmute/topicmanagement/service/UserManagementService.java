package com.hcmute.topicmanagement.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String DEFAULT_ACCOUNT_ROLE_CODE = "STUDENT";
    private static final String STUDENT_EMAIL_DOMAIN = "@student.hcmute.edu.vn";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
        return listUsers(null);
    }

    public List<UserSummary> listUsers(String roleCode) {
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        return userRepository.findAllWithRolesOrderByFullNameAsc().stream()
                .filter(user -> !hasAdminRole(user))
                .filter(user -> normalizedRoleCode == null || matchesDirectoryRole(user, normalizedRoleCode))
                .map(this::toSummary)
                .toList();
    }

    public UserDirectoryPage listUsersPage(String roleCode, String search, int page, int size,
            String sort, String direction) {
        List<UserSummary> filtered = listUsers(roleCode).stream()
                .filter(user -> matchesSearch(user, search))
                .sorted(userComparator(sort, direction))
                .toList();
        int safeSize = Math.min(Math.max(size, 5), 100);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        List<UserSummary> content = filtered.subList(from, to);
        long activeCount = filtered.stream().filter(UserSummary::isActive).count();
        return new UserDirectoryPage(content, safePage, safeSize, totalItems, totalPages,
                activeCount, totalItems - activeCount, normalizeSort(sort), normalizeDirection(direction));
    }

    private static boolean matchesSearch(UserSummary user, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String query = search.trim().toLowerCase(Locale.ROOT);
        return String.join(" ", user.getFullName(), user.getLoginIdentifier(),
                user.getEmailOrCode() == null ? "" : user.getEmailOrCode(),
                user.getStudentCode() == null ? "" : user.getStudentCode()).toLowerCase(Locale.ROOT)
                .contains(query);
    }

    private static Comparator<UserSummary> userComparator(String sort, String direction) {
        Comparator<String> text = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<UserSummary> comparator = switch (normalizeSort(sort)) {
            case "studentCode" -> Comparator.comparing(UserSummary::getStudentCode, text);
            case "email" -> Comparator.comparing(UserSummary::getEmailOrCode, text);
            case "roles" -> Comparator.comparing(user -> user.getRoles().stream()
                    .map(RoleBadge::getName).sorted(String.CASE_INSENSITIVE_ORDER).findFirst().orElse(null), text);
            case "status" -> Comparator.comparing(UserSummary::isActive).reversed();
            default -> Comparator.comparing(UserSummary::getFullName, text)
                    .thenComparing(UserSummary::getLoginIdentifier, text);
        };
        return "desc".equals(normalizeDirection(direction)) ? comparator.reversed() : comparator;
    }

    private static String normalizeSort(String sort) {
        return Set.of("account", "studentCode", "email", "roles", "status")
                .contains(sort) ? sort : "account";
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction) ? "desc" : "asc";
    }

    public List<RoleOption> listAssignableRoles() {
        return roleRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(RoleEntity::isSystemRole)
                .filter(role -> !isAdminRole(role))
                .map(role -> new RoleOption(role.getId(), role.getCode(), role.getName()))
                .toList();
    }

    public List<RoleOption> listAccountCreationRoles() {
        return listAssignableRoles().stream()
                .filter(role -> !"FACULTY_HEAD".equalsIgnoreCase(role.getCode()))
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
                roleIds,
                hasActiveRole(user, DEFAULT_ACCOUNT_ROLE_CODE),
                hasActiveRole(user, DEFAULT_ACCOUNT_ROLE_CODE) ? user.getLoginIdentifier() : null);
    }

    @Transactional
    public void createStudent(
            String fullName,
            String studentCode) {
        String normalizedStudentCode = validateStudentCode(studentCode);
        String normalizedLogin = normalizedStudentCode;
        validateIdentity(normalizedLogin, fullName);
        if (userRepository.existsByLoginIdentifierIgnoreCase(normalizedLogin)) {
            throw new UserValidationException("Student code is already in use as a login identifier.");
        }
        String normalizedEmail = studentEmail(normalizedStudentCode);
        if (userRepository.existsByEmailOrCodeIgnoreCase(normalizedEmail)) {
            throw new UserValidationException("Email address is already in use.");
        }

        UserEntity user = new UserEntity(normalizedLogin, fullName.trim(), null);
        user.setEmailOrCode(normalizedEmail);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        saveRoleAssignments(savedUser, List.of(loadCreationRole(DEFAULT_ACCOUNT_ROLE_CODE)));
    }

    @Transactional
    public void createLecturer(
            String fullName,
            String email) {
        String normalizedEmail = validateEmail(email);
        String normalizedLogin = normalizedEmail;
        validateIdentity(normalizedLogin, fullName);
        if (userRepository.existsByLoginIdentifierIgnoreCase(normalizedLogin)) {
            throw new UserValidationException("Email address is already in use as a login identifier.");
        }
        if (userRepository.existsByEmailOrCodeIgnoreCase(normalizedEmail)) {
            throw new UserValidationException("Email address is already in use.");
        }

        UserEntity user = new UserEntity(
                normalizedLogin,
                fullName.trim(),
                null);
        user.setEmailOrCode(normalizedEmail);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        saveRoleAssignments(savedUser, List.of(loadCreationRole("LECTURER")));
    }

    @Transactional
    public void createUser(
            String loginIdentifier,
            String fullName,
            String emailOrCode,
            String password,
            Set<Long> roleIds,
            String studentCode) {
        validatePassword(password, true);

        List<RoleEntity> roles = validateAndLoadRoles(roleIds, false);
        boolean student = containsRole(roles, DEFAULT_ACCOUNT_ROLE_CODE);
        String normalizedStudentCode = student ? validateStudentCode(studentCode) : null;
        String normalizedLogin = student ? normalizedStudentCode : normalizeLoginIdentifier(loginIdentifier);
        validateIdentity(normalizedLogin, fullName);
        if (userRepository.existsByLoginIdentifierIgnoreCase(normalizedLogin)) {
            throw new UserValidationException(student
                    ? "Student code is already in use as a login identifier."
                    : "Login identifier is already in use.");
        }
        String normalizedEmail = student ? studentEmail(normalizedStudentCode) : validateEmail(emailOrCode);
        if (userRepository.existsByEmailOrCodeIgnoreCase(normalizedEmail)) {
            throw new UserValidationException("Email address is already in use.");
        }
        UserEntity user = new UserEntity(
                normalizedLogin,
                fullName.trim(),
                passwordEncoder.encode(password));
        user.setEmailOrCode(normalizedEmail);
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
        List<RoleEntity> roles = validateAndLoadRoles(roleIds, true);
        boolean student = containsRole(roles, DEFAULT_ACCOUNT_ROLE_CODE);
        String normalizedEmail;
        if (student) {
            normalizedEmail = studentEmail(validateStudentCode(user.getLoginIdentifier()));
        } else {
            normalizedEmail = validateEmail(emailOrCode);
        }
        if (userRepository.existsByEmailOrCodeIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new UserValidationException("Email address is already in use.");
        }

        user.setFullName(fullName.trim());
        user.setEmailOrCode(normalizedEmail);
        if (StringUtils.hasText(newPassword)) {
            validatePassword(newPassword, false);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
        saveRoleAssignments(user, roles);
    }

    @Transactional
    public void setPassword(Long id, String password) {
        UserEntity user = findUserWithRoles(id);
        validatePassword(password, true);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Transactional
    public boolean toggleActive(Long id, String currentIdentity) {
        UserEntity user = findUserWithRoles(id);
        String normalizedCurrentIdentity = normalizeOptional(currentIdentity);
        if (StringUtils.hasText(normalizedCurrentIdentity)
                && (user.getLoginIdentifier().equalsIgnoreCase(normalizedCurrentIdentity)
                        || (StringUtils.hasText(user.getEmailOrCode())
                                && user.getEmailOrCode().equalsIgnoreCase(normalizedCurrentIdentity)))) {
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

    @Transactional
    public void deleteUser(Long id, String currentIdentity, String directoryRoleCode) {
        UserEntity user = findUserWithRoles(id);
        String normalizedCurrentIdentity = normalizeOptional(currentIdentity);
        if (StringUtils.hasText(normalizedCurrentIdentity)
                && (user.getLoginIdentifier().equalsIgnoreCase(normalizedCurrentIdentity)
                        || (StringUtils.hasText(user.getEmailOrCode())
                                && user.getEmailOrCode().equalsIgnoreCase(normalizedCurrentIdentity)))) {
            throw new UserValidationException("You cannot delete your own account.");
        }
        if (!matchesDirectoryRole(user, normalizeRoleCode(directoryRoleCode))) {
            throw new UserValidationException("This account is not part of the selected directory.");
        }

        try {
            userRoleRepository.deleteAllByUser_Id(id);
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new UserValidationException(
                    "This account cannot be deleted because it is referenced by existing records. Lock it instead.");
        }
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

    private List<RoleEntity> validateAndLoadRoles(Set<Long> roleIds, boolean allowFacultyHead) {
        Set<Long> selectedIds = roleIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(roleIds);
        if (selectedIds.isEmpty()) {
            throw new UserValidationException("Select at least one active system role.");
        }

        List<RoleEntity> roles = roleRepository.findAllById(selectedIds);
        if (roles.size() != selectedIds.size()
                || roles.stream().anyMatch(role -> !role.isSystemRole() || !role.isActive()
                        || isAdminRole(role)
                        || (!allowFacultyHead && isFacultyHeadRole(role)))) {
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
                hasActiveRole(user, "STUDENT") ? user.getLoginIdentifier() : null,
                hasActiveRole(user, "STUDENT"),
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

    private RoleEntity loadCreationRole(String code) {
        return roleRepository.findByCode(code)
                .filter(role -> role.isSystemRole() && role.isActive())
                .orElseThrow(() -> new UserValidationException("The required account role is not available."));
    }

    private static String validateEmail(String value) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw new UserValidationException("Email address is required for non-Student accounts.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new UserValidationException("Enter a valid email address.");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String validateStudentCode(String value) {
        String normalized = normalizeStudentCode(value);
        if (!normalized.matches("[0-9]{8}")) {
            throw new UserValidationException("Student code must contain exactly 8 digits.");
        }
        return normalized;
    }

    private String normalizeStudentCode(String value) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw new UserValidationException("Student code is required for Student accounts.");
        }
        return normalized;
    }

    private static String studentEmail(String studentCode) {
        return studentCode + STUDENT_EMAIL_DOMAIN;
    }

    private static boolean containsRole(List<RoleEntity> roles, String roleCode) {
        return roles.stream().anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
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

    private static boolean matchesDirectoryRole(UserEntity user, String roleCode) {
        if ("LECTURER".equalsIgnoreCase(roleCode)) {
            return hasActiveRole(user, "LECTURER") || hasActiveRole(user, "FACULTY_HEAD");
        }
        return hasActiveRole(user, roleCode);
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

    private static boolean isFacultyHeadRole(RoleEntity role) {
        return "FACULTY_HEAD".equalsIgnoreCase(role.getCode());
    }

    private static String normalizeLoginIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRoleCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    public static final class UserDirectoryPage {
        private final List<UserSummary> content;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final long activeCount;
        private final long lockedCount;
        private final String sort;
        private final String direction;

        public UserDirectoryPage(List<UserSummary> content, int page, int size, int totalItems,
                int totalPages, long activeCount, long lockedCount, String sort, String direction) {
            this.content = List.copyOf(content);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.activeCount = activeCount;
            this.lockedCount = lockedCount;
            this.sort = sort;
            this.direction = direction;
        }

        public List<UserSummary> getContent() { return content; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public long getActiveCount() { return activeCount; }
        public long getLockedCount() { return lockedCount; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class UserSummary {
        private final Long id;
        private final String loginIdentifier;
        private final String fullName;
        private final String emailOrCode;
        private final String studentCode;
        private final boolean student;
        private final boolean active;
        private final String initials;
        private final List<RoleBadge> roles;

        public UserSummary(Long id, String loginIdentifier, String fullName, String emailOrCode,
                String studentCode, boolean student,
                boolean active, String initials, List<RoleBadge> roles) {
            this.id = id;
            this.loginIdentifier = loginIdentifier;
            this.fullName = fullName;
            this.emailOrCode = emailOrCode;
            this.studentCode = studentCode;
            this.student = student;
            this.active = active;
            this.initials = initials;
            this.roles = List.copyOf(roles);
        }

        public Long getId() { return id; }
        public String getLoginIdentifier() { return loginIdentifier; }
        public String getFullName() { return fullName; }
        public String getEmailOrCode() { return emailOrCode; }
        public String getStudentCode() { return studentCode; }
        public boolean isStudent() { return student; }
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
        private final boolean student;
        private final String studentCode;

        public UserEditorData(Long id, String loginIdentifier, String fullName, String emailOrCode,
                boolean active, Set<Long> roleIds,
                boolean student, String studentCode) {
            this.id = id;
            this.loginIdentifier = loginIdentifier;
            this.fullName = fullName;
            this.emailOrCode = emailOrCode;
            this.active = active;
            this.roleIds = new LinkedHashSet<>(roleIds);
            this.student = student;
            this.studentCode = studentCode;
        }

        public Long getId() { return id; }
        public String getLoginIdentifier() { return loginIdentifier; }
        public String getFullName() { return fullName; }
        public String getEmailOrCode() { return emailOrCode; }
        public boolean isActive() { return active; }
        public Set<Long> getRoleIds() { return roleIds; }
        public boolean isStudent() { return student; }
        public String getStudentCode() { return studentCode; }
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
