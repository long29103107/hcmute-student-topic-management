package com.hcmute.topicmanagement.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 150;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");
    private static final Pattern NAME_CONTENT_PATTERN = Pattern.compile(".*[\\p{L}\\p{N}].*");

    private final DepartmentRepository departmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            TopicRepository topicRepository,
            UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public List<DepartmentSummary> listDepartments() {
        return listDepartments("", "name", "asc");
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public List<DepartmentSummary> listDepartments(String search, String sort, String direction) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Comparator<DepartmentSummary> comparator = switch (normalizeSort(sort)) {
            case "code" -> Comparator.comparing(
                    DepartmentSummary::getCode, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(DepartmentSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(DepartmentSummary::isActive)
                    .reversed()
                    .thenComparing(DepartmentSummary::getName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    DepartmentSummary::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(DepartmentSummary::getCode, String.CASE_INSENSITIVE_ORDER);
        };
        if ("desc".equals(normalizeDirection(direction))) {
            comparator = comparator.reversed();
        }

        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(DepartmentService::toSummary)
                .filter(department -> normalizedSearch.isBlank()
                        || department.getCode().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || department.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || (department.isActive() ? "active" : "inactive").contains(normalizedSearch))
                .sorted(comparator)
                .toList();
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentPage listDepartmentsPage(String search, int page, int size, String sort, String direction) {
        List<DepartmentSummary> filtered = listDepartments(search, sort, direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new DepartmentPage(
                filtered.subList(from, to),
                safePage,
                safeSize,
                totalItems,
                totalPages,
                normalizeSort(sort),
                normalizeDirection(direction));
    }

    private static DepartmentSummary toSummary(DepartmentEntity department) {
        return new DepartmentSummary(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.isActive());
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "code", "status" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "name";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    /** Department options used by the administrator account assignment forms. */
    @PreAuthorize("hasRole('ADMIN')")
    public List<DepartmentSummary> listDepartmentsForAssignment() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .filter(DepartmentEntity::isActive)
                .map(DepartmentService::toSummary)
                .toList();
    }

    /** Read-only lookup contract for period/topic services. */
    public Optional<DepartmentEntity> findActiveById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return departmentRepository.findById(id).filter(DepartmentEntity::isActive);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_HEAD')")
    public DepartmentOverview getDepartmentForFacultyHead(String email) {
        UserEntity facultyHead = userRepository.findByEmailIgnoreCaseWithDepartment(email).orElse(null);
        DepartmentEntity department = facultyHead == null ? null : facultyHead.getDepartment();
        if (department == null) {
            return new DepartmentOverview(null, null, null, List.of());
        }

        List<DepartmentMember> members = userRepository
                .findActiveByDepartmentIdWithRolesOrderByFullNameAsc(department.getId())
                .stream()
                .map(DepartmentService::toDepartmentMember)
                .toList();
        return new DepartmentOverview(department.getId(), department.getCode(), department.getName(), members);
    }

    private static DepartmentMember toDepartmentMember(UserEntity user) {
        String roleNames = user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.isActive())
                .map(RoleEntity::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return new DepartmentMember(
                user.getFullName(),
                user.getLoginIdentifier(),
                user.getEmailOrCode(),
                roleNames,
                user.isActive());
    }

    @Transactional
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentEntity create(String code, String name) {
        DepartmentInput input = validateAndNormalize(code, name);
        if (departmentRepository.existsByCodeIgnoreCase(input.code())) {
            throw new DepartmentValidationException("Department code is already in use.");
        }
        if (departmentRepository.existsByNameIgnoreCase(input.name())) {
            throw new DepartmentValidationException("Department name is already in use.");
        }
        return save(input, new DepartmentEntity(input.code(), input.name()));
    }

    @Transactional
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public DepartmentEntity update(Long id, String code, String name) {
        DepartmentEntity department = findDepartment(id);
        DepartmentInput input = validateAndNormalize(code, name);
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(input.code(), id)) {
            throw new DepartmentValidationException("Department code is already in use.");
        }
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(input.name(), id)) {
            throw new DepartmentValidationException("Department name is already in use.");
        }
        department.setCode(input.code());
        department.setName(input.name());
        return save(input, department);
    }

    @Transactional
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public boolean toggleActive(Long id) {
        DepartmentEntity department = findDepartment(id);
        department.setActive(!department.isActive());
        return department.isActive();
    }

    @Transactional
    @PreAuthorize("hasAuthority('DEPARTMENT_MANAGE')")
    public void delete(Long id) {
        DepartmentEntity department = findDepartment(id);
        if (userRepository.existsByDepartment_Id(id)) {
            throw new DepartmentValidationException(
                    "This department cannot be deleted because it still has assigned users.");
        }
        if (topicRepository.existsByDepartment_Id(id)) {
            throw new DepartmentValidationException(
                    "This department cannot be deleted because it is referenced by existing topics.");
        }

        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new DepartmentValidationException(
                    "This department cannot be deleted because it is still in use.", exception);
        }
    }

    private DepartmentEntity save(DepartmentInput input, DepartmentEntity department) {
        try {
            return departmentRepository.saveAndFlush(department);
        } catch (DataIntegrityViolationException exception) {
            throw new DepartmentValidationException(
                    "Department code and name must both be unique.", exception);
        }
    }

    private DepartmentEntity findDepartment(Long id) {
        if (id == null) {
            throw new DepartmentNotFoundException(null);
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    private static DepartmentInput validateAndNormalize(String code, String name) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedCode.isBlank()) {
            throw new DepartmentValidationException("Department code is required.");
        }
        if (normalizedCode.length() > MAX_CODE_LENGTH || !CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new DepartmentValidationException(
                    "Department code may contain letters, numbers, hyphens and underscores only, up to 30 characters.");
        }
        if (normalizedName.isBlank()) {
            throw new DepartmentValidationException("Department name is required.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH || !NAME_CONTENT_PATTERN.matcher(normalizedName).matches()) {
            throw new DepartmentValidationException(
                    "Department name must contain a letter or number and be at most 150 characters.");
        }
        return new DepartmentInput(normalizedCode, normalizedName);
    }

    public static final class DepartmentSummary {
        private final Long id;
        private final String code;
        private final String name;
        private final boolean active;

        public DepartmentSummary(Long id, String code, String name, boolean active) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.active = active;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static final class DepartmentPage {
        private final List<DepartmentSummary> content;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String sort;
        private final String direction;

        public DepartmentPage(List<DepartmentSummary> content, int page, int size, int totalItems,
                int totalPages, String sort, String direction) {
            this.content = List.copyOf(content);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.sort = sort;
            this.direction = direction;
        }

        public List<DepartmentSummary> getContent() {
            return content;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public String getSort() {
            return sort;
        }

        public String getDirection() {
            return direction;
        }

        public boolean isHasPrevious() {
            return page > 0;
        }

        public boolean isHasNext() {
            return page + 1 < totalPages;
        }
    }

    public static final class DepartmentOverview {
        private final Long id;
        private final String code;
        private final String name;
        private final List<DepartmentMember> members;

        public DepartmentOverview(Long id, String code, String name, List<DepartmentMember> members) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.members = members;
        }

        public boolean isAssigned() {
            return id != null;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public List<DepartmentMember> getMembers() {
            return members;
        }
    }

    public static final class DepartmentMember {
        private final String fullName;
        private final String loginIdentifier;
        private final String emailOrCode;
        private final String roleNames;
        private final boolean active;

        public DepartmentMember(
                String fullName,
                String loginIdentifier,
                String emailOrCode,
                String roleNames,
                boolean active) {
            this.fullName = fullName;
            this.loginIdentifier = loginIdentifier;
            this.emailOrCode = emailOrCode;
            this.roleNames = roleNames;
            this.active = active;
        }

        public String getFullName() {
            return fullName;
        }

        public String getLoginIdentifier() {
            return loginIdentifier;
        }

        public String getEmailOrCode() {
            return emailOrCode;
        }

        public String getRoleNames() {
            return roleNames;
        }

        public boolean isActive() {
            return active;
        }
    }

    private record DepartmentInput(String code, String name) {
    }

    public static class DepartmentNotFoundException extends RuntimeException {
        public DepartmentNotFoundException(Long id) {
            super(id == null ? "Department id is required." : "Department not found: " + id);
        }
    }

    public static class DepartmentValidationException extends RuntimeException {
        public DepartmentValidationException(String message) {
            super(message);
        }

        public DepartmentValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
