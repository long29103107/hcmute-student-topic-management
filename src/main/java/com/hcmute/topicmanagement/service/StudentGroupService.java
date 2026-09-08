package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class StudentGroupService {

    public static final int MAX_MEMBERS = 3;

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final RegistrationPeriodService registrationPeriodService;

    public StudentGroupService(
            StudentGroupRepository studentGroupRepository,
            UserRepository userRepository,
            RegistrationPeriodRepository registrationPeriodRepository,
            RegistrationPeriodService registrationPeriodService) {
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.registrationPeriodService = registrationPeriodService;
    }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupPage listPage(
            String studentEmail, String search, int page, int size, String sort, String direction) {
        UserEntity student = findActiveStudent(studentEmail);
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);

        List<StudentGroupSummary> filtered = studentGroupRepository
                .findRelatedGroupsWithDetails(student.getId()).stream()
                .map(group -> toSummary(group, student.getId()))
                .filter(group -> matchesSearch(group, normalizedSearch))
                .sorted(groupComparator(normalizedSort, normalizedDirection))
                .toList();

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new StudentGroupPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection);
    }

    @PreAuthorize("hasAuthority('GROUP_READ')")
    public GroupDirectoryPage listFacultyPage(
            String actorEmail, String search, int page, int size, String sort, String direction) {
        UserEntity actor = findActiveActor(actorEmail);
        GroupDirectoryScope scope = groupDirectoryScope(actor);
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);

        List<StudentGroupSummary> filtered = (scope.isAdmin()
                ? studentGroupRepository.findAllWithDetails()
                : scope.hasDepartmentScope()
                        ? studentGroupRepository.findForDepartmentWithDetails(scope.departmentId())
                        : List.<StudentGroupEntity>of()).stream()
                .map(group -> toSummary(group, null))
                .filter(group -> matchesSearch(group, normalizedSearch))
                .sorted(groupComparator(normalizedSort, normalizedDirection))
                .toList();

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new GroupDirectoryPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection, scope.label());
    }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public List<PeriodOption> listOpenPeriodOptions() {
        return registrationPeriodService.listOpenForStudent(LocalDateTime.now()).stream()
                .map(period -> new PeriodOption(period.getId(), period.getName(), period.getType().name()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary create(String studentEmail, String name, Long periodId) {
        UserEntity student = findActiveStudent(studentEmail);
        String normalizedName = normalizeName(name);
        RegistrationPeriodEntity period = requireOpenStudentPeriod(periodId);
        ensureNoActiveMembership(student, period);

        StudentGroupEntity group = new StudentGroupEntity(normalizedName, period, student, student);
        return toSummary(studentGroupRepository.saveAndFlush(group), student.getId());
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary join(String studentEmail, Long groupId) {
        UserEntity student = findActiveStudent(studentEmail);
        StudentGroupEntity group = findGroup(groupId);
        ensureActive(group);
        requireOpenStudentPeriod(group.getRegistrationPeriod().getId());
        ensureLeaderInvariant(group);

        if (isMember(group, student.getId())) {
            throw new StudentGroupValidationException("You are already a member of this group.");
        }
        if (group.getMembers().size() >= MAX_MEMBERS) {
            throw new StudentGroupValidationException("A student group cannot have more than 3 members.");
        }
        ensureNoActiveMembershipInPeriod(student, group.getRegistrationPeriod().getId());

        group.getMembers().add(student);
        return toSummary(studentGroupRepository.saveAndFlush(group), student.getId());
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary leave(String studentEmail, Long groupId) {
        UserEntity student = findActiveStudent(studentEmail);
        StudentGroupEntity group = findGroup(groupId);
        ensureActive(group);
        lockPeriod(group.getRegistrationPeriod().getId());
        ensureLeaderInvariant(group);
        if (!isMember(group, student.getId())) {
            throw new StudentGroupAccessException("You can only leave a group that you belong to.");
        }
        if (group.getLeader() != null && student.getId().equals(group.getLeader().getId())) {
            throw new StudentGroupValidationException(
                    "The group leader cannot leave without a leader transition.");
        }

        group.getMembers().removeIf(member -> student.getId().equals(member.getId()));
        return toSummary(studentGroupRepository.saveAndFlush(group), student.getId());
    }

    @Transactional
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary transferLeader(String actorEmail, Long groupId, Long newLeaderId) {
        UserEntity actor = findActiveActor(actorEmail);
        StudentGroupEntity group = findGroup(groupId);
        ensureActive(group);
        lockPeriod(group.getRegistrationPeriod().getId());
        ensureLeaderInvariant(group);

        if (newLeaderId == null) {
            throw new StudentGroupValidationException("Select a group member as the new leader.");
        }
        UserEntity newLeader = group.getMembers().stream()
                .filter(member -> newLeaderId.equals(member.getId()))
                .findFirst()
                .orElseThrow(() -> new StudentGroupValidationException(
                        "The new leader must be a member of this group."));
        if (!isActiveStudent(newLeader)) {
            throw new StudentGroupValidationException(
                    "The new leader must be an active student in this group.");
        }
        if (group.getLeader() != null && newLeaderId.equals(group.getLeader().getId())) {
            throw new StudentGroupValidationException("The selected student is already the group leader.");
        }
        if (hasActiveRole(actor, "STUDENT")) {
            if (!isMember(group, actor.getId())) {
                throw new StudentGroupAccessException("You can only manage a group that you belong to.");
            }
            if (group.getLeader() == null || !actor.getId().equals(group.getLeader().getId())) {
                throw new StudentGroupAccessException("Only the current group leader can transfer leadership.");
            }
        }

        group.setLeader(newLeader);
        return toSummary(
                studentGroupRepository.saveAndFlush(group),
                hasActiveRole(actor, "STUDENT") ? actor.getId() : null);
    }

    private UserEntity findActiveStudent(String email) {
        UserEntity student = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new StudentGroupAccessException("Student account is not available."));
        if (!hasActiveRole(student, "STUDENT")) {
            throw new StudentGroupAccessException("Only active students can manage student groups.");
        }
        return student;
    }

    private UserEntity findActiveActor(String email) {
        UserEntity actor = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new StudentGroupAccessException("The group management account is unavailable."));
        if (actor.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .noneMatch(role -> role != null && role.isActive())) {
            throw new StudentGroupAccessException("The group management account has no active role.");
        }
        return actor;
    }

    private static GroupDirectoryScope groupDirectoryScope(UserEntity actor) {
        if (hasActiveRole(actor, "ADMIN")) {
            return new GroupDirectoryScope(true, null, "Admin · all departments");
        }
        if (!hasActiveRole(actor, "FACULTY_HEAD")) {
            throw new StudentGroupAccessException("Only Admin or Faculty Head can view student groups.");
        }
        DepartmentEntity department = actor.getDepartment();
        if (department == null || department.getId() == null) {
            return new GroupDirectoryScope(false, null, "Faculty Head · no department assigned");
        }
        return new GroupDirectoryScope(false, department.getId(),
                "Faculty Head · " + department.getCode() + " · " + department.getName());
    }

    private RegistrationPeriodEntity requireOpenStudentPeriod(Long periodId) {
        if (periodId == null) {
            throw new StudentGroupValidationException("Select a registration period.");
        }
        RegistrationPeriodEntity lockedPeriod = lockPeriod(periodId);
        try {
            registrationPeriodService.requireOpenForStudent(lockedPeriod.getId(), LocalDateTime.now());
            return lockedPeriod;
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException
                | RegistrationPeriodService.RegistrationPeriodAccessException exception) {
            throw new StudentGroupValidationException(
                    "The selected registration period is not open for student groups.");
        }
    }

    private RegistrationPeriodEntity lockPeriod(Long periodId) {
        if (periodId == null) {
            throw new StudentGroupValidationException("Registration period is required.");
        }
        return registrationPeriodRepository.findByIdForGroupMutation(periodId)
                .orElseThrow(() -> new StudentGroupValidationException(
                        "The registration period is no longer available."));
    }

    private StudentGroupEntity findGroup(Long groupId) {
        if (groupId == null) {
            throw new StudentGroupValidationException("Group id is required.");
        }
        return studentGroupRepository.findByIdForMutation(groupId)
                .orElseThrow(() -> new StudentGroupNotFoundException(groupId));
    }

    private void ensureActive(StudentGroupEntity group) {
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new StudentGroupValidationException(
                    "Only active groups can be joined, left or have their leader changed.");
        }
    }

    private static void ensureLeaderInvariant(StudentGroupEntity group) {
        if (group.getStatus() == GroupStatus.ACTIVE
                && (group.getLeader() == null
                        || !isMember(group, group.getLeader().getId())
                        || !isActiveStudent(group.getLeader()))) {
            throw new StudentGroupValidationException(
                    "An active group must have one active student leader who is also a member.");
        }
    }

    private void ensureNoActiveMembership(UserEntity student, RegistrationPeriodEntity period) {
        ensureNoActiveMembershipInPeriod(student, period.getId());
    }

    private void ensureNoActiveMembershipInPeriod(UserEntity student, Long periodId) {
        if (studentGroupRepository.existsActiveMembership(periodId, GroupStatus.ACTIVE, student.getId())) {
            throw new StudentGroupValidationException(
                    "A student can belong to only one active group in the same registration period.");
        }
    }

    private static boolean isMember(StudentGroupEntity group, Long studentId) {
        return studentId != null && group.getMembers().stream().anyMatch(member -> studentId.equals(member.getId()));
    }

    private static boolean isActiveStudent(UserEntity user) {
        return user != null && user.isActive() && hasActiveRole(user, "STUDENT");
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.isActive())
                .map(RoleEntity::getCode)
                .anyMatch(roleCode::equalsIgnoreCase);
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new StudentGroupValidationException("Group name is required.");
        }
        if (normalized.length() > 150) {
            throw new StudentGroupValidationException("Group name must be at most 150 characters.");
        }
        return normalized;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesSearch(StudentGroupSummary group, String search) {
        if (search.isBlank()) {
            return true;
        }
        return contains(group.getName(), search)
                || contains(group.getPeriodName(), search)
                || contains(group.getLeaderName(), search)
                || contains(group.getLeaderLogin(), search)
                || group.getMembers().stream().anyMatch(member ->
                        contains(member.fullName(), search) || contains(member.login(), search));
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<StudentGroupSummary> groupComparator(String sort, String direction) {
        Comparator<StudentGroupSummary> comparator = switch (sort) {
            case "period" -> Comparator.comparing(
                    StudentGroupSummary::getPeriodName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "leader" -> Comparator.comparing(
                    StudentGroupSummary::getLeaderName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "members" -> Comparator.comparingInt(StudentGroupSummary::getMemberCount)
                    .thenComparing(StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(
                    StudentGroupSummary::getStatusCode, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "created" -> Comparator.comparing(
                    StudentGroupSummary::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    StudentGroupSummary::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentGroupSummary::getId);
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSort(String sort) {
        String normalized = sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "period", "leader", "members", "status", "created" -> normalized;
            default -> "group";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private static StudentGroupSummary toSummary(StudentGroupEntity group, Long currentStudentId) {
        List<MemberSummary> members = group.getMembers().stream()
                .sorted(Comparator.comparing(UserEntity::getFullName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UserEntity::getLoginIdentifier, String.CASE_INSENSITIVE_ORDER))
                .map(member -> new MemberSummary(
                        member.getId(), member.getFullName(), member.getEmailOrCode(),
                        group.getLeader() != null && member.getId().equals(group.getLeader().getId())))
                .toList();
        boolean currentStudentIsMember = currentStudentId != null
                && members.stream().anyMatch(member -> currentStudentId.equals(member.id()));
        boolean currentStudentIsLeader = group.getLeader() != null
                && currentStudentId != null
                && currentStudentId.equals(group.getLeader().getId());
        return new StudentGroupSummary(
                group.getId(), group.getName(), group.getRegistrationPeriod().getId(),
                group.getRegistrationPeriod().getName(), group.getRegistrationPeriod().getType().name(),
                group.getStatus().name(), statusLabel(group.getStatus()), group.getCreatedBy().getFullName(),
                group.getLeader().getId(), group.getLeader().getFullName(), group.getLeader().getEmailOrCode(),
                members, group.getCreatedAt(), currentStudentIsMember, currentStudentIsLeader);
    }

    private static String statusLabel(GroupStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case INACTIVE -> "Inactive";
        };
    }

    public record PeriodOption(Long id, String name, String type) {
    }

    private record GroupDirectoryScope(boolean admin, Long departmentId, String label) {
        boolean isAdmin() {
            return admin;
        }

        boolean hasDepartmentScope() {
            return departmentId != null;
        }
    }

    public static final class GroupDirectoryPage {
        private final List<StudentGroupSummary> groups;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final String scopeLabel;

        public GroupDirectoryPage(
                List<StudentGroupSummary> groups, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction, String scopeLabel) {
            this.groups = List.copyOf(groups);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
            this.scopeLabel = scopeLabel;
        }

        public List<StudentGroupSummary> getGroups() { return groups; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public String getScopeLabel() { return scopeLabel; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class StudentGroupPage {
        private final List<StudentGroupSummary> groups;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;

        public StudentGroupPage(
                List<StudentGroupSummary> groups, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction) {
            this.groups = List.copyOf(groups);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
        }

        public List<StudentGroupSummary> getGroups() { return groups; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class StudentGroupSummary {
        private final Long id;
        private final String name;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final String statusCode;
        private final String statusLabel;
        private final String createdByName;
        private final Long leaderId;
        private final String leaderName;
        private final String leaderLogin;
        private final List<MemberSummary> members;
        private final LocalDateTime createdAt;
        private final boolean currentStudentIsMember;
        private final boolean currentStudentIsLeader;

        public StudentGroupSummary(
                Long id, String name, Long periodId, String periodName, String periodType,
                String statusCode, String statusLabel, String createdByName, Long leaderId,
                String leaderName, String leaderLogin, List<MemberSummary> members, LocalDateTime createdAt,
                boolean currentStudentIsMember, boolean currentStudentIsLeader) {
            this.id = id;
            this.name = name;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.createdByName = createdByName;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
            this.leaderLogin = leaderLogin;
            this.members = List.copyOf(members);
            this.createdAt = createdAt;
            this.currentStudentIsMember = currentStudentIsMember;
            this.currentStudentIsLeader = currentStudentIsLeader;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public String getCreatedByName() { return createdByName; }
        public Long getLeaderId() { return leaderId; }
        public String getLeaderName() { return leaderName; }
        public String getLeaderLogin() { return leaderLogin; }
        public List<MemberSummary> getMembers() { return members; }
        public int getMemberCount() { return members.size(); }
        public int getMaxMembers() { return MAX_MEMBERS; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isCurrentStudentIsMember() { return currentStudentIsMember; }
        public boolean isCurrentStudentIsLeader() { return currentStudentIsLeader; }
        public boolean isCanLeave() {
            return GroupStatus.ACTIVE.name().equals(statusCode)
                    && currentStudentIsMember && !currentStudentIsLeader;
        }
        public boolean isCanTransfer() {
            return GroupStatus.ACTIVE.name().equals(statusCode)
                    && currentStudentIsLeader && members.size() > 1;
        }
    }

    public record MemberSummary(Long id, String fullName, String login, boolean leader) {
    }

    public static class StudentGroupNotFoundException extends RuntimeException {
        public StudentGroupNotFoundException(Long id) {
            super(id == null ? "Group id is required." : "Student group not found: " + id);
        }
    }

    public static class StudentGroupValidationException extends RuntimeException {
        public StudentGroupValidationException(String message) {
            super(message);
        }
    }

    public static class StudentGroupAccessException extends RuntimeException {
        public StudentGroupAccessException(String message) {
            super(message);
        }
    }
}
