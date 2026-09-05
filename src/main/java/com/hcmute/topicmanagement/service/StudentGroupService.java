package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class StudentGroupService {

    public static final int MAX_MEMBERS = 3;

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodService registrationPeriodService;

    public StudentGroupService(
            StudentGroupRepository studentGroupRepository,
            UserRepository userRepository,
            RegistrationPeriodService registrationPeriodService) {
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
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

    private UserEntity findActiveStudent(String email) {
        UserEntity student = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new StudentGroupAccessException("Student account is not available."));
        if (!hasActiveRole(student, "STUDENT")) {
            throw new StudentGroupAccessException("Only active students can manage student groups.");
        }
        return student;
    }

    private RegistrationPeriodEntity requireOpenStudentPeriod(Long periodId) {
        if (periodId == null) {
            throw new StudentGroupValidationException("Select a registration period.");
        }
        try {
            return registrationPeriodService.requireOpenForStudent(periodId, LocalDateTime.now());
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException
                | RegistrationPeriodService.RegistrationPeriodAccessException exception) {
            throw new StudentGroupValidationException(
                    "The selected registration period is not open for student groups.");
        }
    }

    private StudentGroupEntity findGroup(Long groupId) {
        if (groupId == null) {
            throw new StudentGroupValidationException("Group id is required.");
        }
        return studentGroupRepository.findByIdWithDetails(groupId)
                .orElseThrow(() -> new StudentGroupNotFoundException(groupId));
    }

    private void ensureActive(StudentGroupEntity group) {
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new StudentGroupValidationException("Only active groups can be joined or left.");
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
        return group.getMembers().stream().anyMatch(member -> studentId.equals(member.getId()));
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
        boolean currentStudentIsMember = members.stream().anyMatch(member -> currentStudentId.equals(member.id()));
        boolean currentStudentIsLeader = group.getLeader() != null
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
        public boolean isCanLeave() { return currentStudentIsMember && !currentStudentIsLeader; }
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
