package com.hcmute.topicmanagement.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicSupervisorService {

    private static final int MIN_SUPERVISORS = 1;
    private static final int MAX_SUPERVISORS = 2;

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicSupervisorService(TopicRepository topicRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicAssignmentPage listManageableTopics(String actorEmail) {
        return listManageableTopics(actorEmail, "", null, null, "", 0, 20, "topic", "asc");
    }

    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicAssignmentPage listManageableTopics(
            String actorEmail, String search, int page, int size, String sort, String direction) {
        return listManageableTopics(actorEmail, search, null, null, "", page, size, sort, direction);
    }

    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicAssignmentPage listManageableTopics(
            String actorEmail, String search, Long departmentId, Long periodId, String status,
            int page, int size, String sort, String direction) {
        UserEntity actor = findActiveActor(actorEmail);
        ManagementScope scope = scopeFor(actor);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        TopicStatus selectedStatus = parseStatus(status);
        String normalizedStatus = selectedStatus == null ? "" : selectedStatus.name();
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        if (!scope.hasDepartmentScope()) {
            return new TopicAssignmentPage(
                    List.of(), 0, safeSize, 0, 1, normalizedSearch, departmentId, periodId, normalizedStatus,
                    normalizedSort, normalizedDirection, scope.label(), List.of(), List.of(),
                    java.util.Arrays.stream(TopicStatus.values()).map(Enum::name).toList());
        }

        Long scopeDepartmentId = scope.isAdmin() ? null : scope.departmentId();
        Map<Long, List<SupervisorOption>> optionsByDepartment = userRepository
                .findActiveLecturerCapabilitiesOrderByFullName().stream()
                .filter(user -> user.getDepartment() != null && user.getDepartment().getId() != null)
                .map(TopicSupervisorService::toOption)
                .collect(Collectors.groupingBy(
                        SupervisorOption::getDepartmentId,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));
        List<DepartmentFilterOption> departmentOptions = topicRepository
                .findSupervisorManagementDepartments(scopeDepartmentId).stream()
                .map(department -> new DepartmentFilterOption(department.getId(), department.getCode(), department.getName()))
                .toList();
        List<PeriodFilterOption> periodOptions = topicRepository
                .findSupervisorManagementPeriods(scopeDepartmentId).stream()
                .map(period -> new PeriodFilterOption(period.getId(), period.getName(), period.getType().name()))
                .toList();
        int totalItems = Math.toIntExact(topicRepository.countSupervisorManagementTopics(
                scopeDepartmentId, departmentId, periodId, selectedStatus, normalizedSearch));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
Pageable pageable;

List<Long> topicIds;

if ("supervisors".equals(normalizedSort)) {
    pageable = PageRequest.of(safePage, safeSize);

    topicIds = topicRepository.findSupervisorManagementIdsSortedBySupervisor(
            scopeDepartmentId,
            departmentId,
            periodId,
            selectedStatus,
            normalizedSearch,
            "asc".equals(normalizedDirection),
            pageable
    );
} else {
    pageable = pageableFor(
            safePage,
            safeSize,
            normalizedSort,
            normalizedDirection
    );

    topicIds = topicRepository.findSupervisorManagementIds(
            scopeDepartmentId,
            departmentId,
            periodId,
            selectedStatus,
            normalizedSearch,
            pageable
    );
}
        Map<Long, TopicEntity> topicsById = topicRepository.findAllByIdForSupervisorManagement(topicIds).stream()
                .collect(Collectors.toMap(TopicEntity::getId, topic -> topic));
        List<TopicSummary> topicSummaries = topicIds.stream()
                .map(topicsById::get)
                .filter(java.util.Objects::nonNull)
                .map(topic -> toSummary(
                        topic, optionsByDepartment.getOrDefault(topic.getDepartment().getId(), List.of())))
                .toList();
        return new TopicAssignmentPage(
                topicSummaries,
                safePage,
                safeSize,
                totalItems,
                totalPages,
                normalizedSearch,
                departmentId,
                periodId,
                normalizedStatus,
                normalizedSort,
                normalizedDirection,
                scope.label(),
                departmentOptions,
                periodOptions,
                java.util.Arrays.stream(TopicStatus.values()).map(Enum::name).toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicSummary assignSupervisors(String actorEmail, Long topicId, List<Long> lecturerIds) {
        UserEntity actor = findActiveActor(actorEmail);
        ManagementScope scope = scopeFor(actor);
        TopicEntity topic = topicRepository.findByIdForSupervisorManagement(topicId)
                .orElseThrow(() -> new TopicSupervisorNotFoundException(topicId));
        assertCanManage(scope, topic);

        List<Long> normalizedIds = normalizeIds(lecturerIds);
        List<UserEntity> candidates = userRepository.findActiveLecturerCapabilitiesByIdIn(normalizedIds);
        Map<Long, UserEntity> candidatesById = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, user -> user));
        if (candidatesById.size() != normalizedIds.size()) {
            throw new TopicSupervisorValidationException(
                    "Every selected supervisor must be an active Lecturer or Faculty Head.");
        }
        Long topicDepartmentId = topic.getDepartment().getId();
        if (candidates.stream().anyMatch(candidate -> candidate.getDepartment() == null
                || !topicDepartmentId.equals(candidate.getDepartment().getId()))) {
            throw new TopicSupervisorValidationException(
                    "Every selected supervisor must belong to the topic's department.");
        }

        topic.getSupervisors().clear();
        normalizedIds.forEach(id -> topic.getSupervisors().add(candidatesById.get(id)));
        return toSummary(
                topicRepository.saveAndFlush(topic),
                supervisorOptionsForDepartment(topic.getDepartment().getId()));
    }

    private UserEntity findActiveActor(String actorEmail) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(actorEmail)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new TopicSupervisorAccessException("The supervisor manager account is unavailable."));
    }

    private static ManagementScope scopeFor(UserEntity actor) {
        if (hasActiveRole(actor, "ADMIN")) {
            return new ManagementScope(true, null, "Admin · all departments");
        }
        if (!hasActiveRole(actor, "FACULTY_HEAD")) {
            throw new TopicSupervisorAccessException("Only Admin or Faculty Head can manage topic supervisors.");
        }
        DepartmentEntity department = actor.getDepartment();
        if (department == null || department.getId() == null) {
            return new ManagementScope(false, null, "Faculty Head · no department assigned");
        }
        return new ManagementScope(false, department.getId(),
                "Faculty Head · " + department.getCode() + " · " + department.getName());
    }

    private static void assertCanManage(ManagementScope scope, TopicEntity topic) {
        if (!scope.isAdmin() && (!scope.hasDepartmentScope()
                || topic.getDepartment() == null
                || !scope.departmentId().equals(topic.getDepartment().getId()))) {
            throw new TopicSupervisorAccessException(
                    "Faculty Heads can only manage supervisors for topics in their department.");
        }
    }

    private static List<Long> normalizeIds(List<Long> lecturerIds) {
        if (lecturerIds == null || lecturerIds.isEmpty()) {
            throw new TopicSupervisorValidationException("Select at least one supervisor.");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long lecturerId : lecturerIds) {
            if (lecturerId == null || lecturerId <= 0) {
                throw new TopicSupervisorValidationException("Supervisor id is invalid.");
            }
            if (!uniqueIds.add(lecturerId)) {
                throw new TopicSupervisorValidationException("A supervisor cannot be selected more than once.");
            }
        }
        if (uniqueIds.size() < MIN_SUPERVISORS) {
            throw new TopicSupervisorValidationException("Select at least one supervisor.");
        }
        if (uniqueIds.size() > MAX_SUPERVISORS) {
            throw new TopicSupervisorValidationException("A topic can have at most two supervisors.");
        }
        return new ArrayList<>(uniqueIds);
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(RoleEntity::isActive)
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static Pageable pageableFor(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = "desc".equals(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortOrder = switch (sort) {
            case "department" -> Sort.by(sortDirection, "department.code").and(Sort.by(sortDirection, "department.name"));
            case "period" -> Sort.by(sortDirection, "registrationPeriod.name");
            case "status" -> Sort.by(sortDirection, "status");
            case "proposer" -> Sort.by(sortDirection, "proposedBy.fullName");
            case "supervisors" -> Sort.by(sortDirection, "id");
            default -> Sort.by(sortDirection, "title");
        };
        return PageRequest.of(page, size, sortOrder.and(Sort.by(sortDirection, "id")));
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "department", "period", "status", "proposer", "supervisors" ->
                    sort.trim().toLowerCase(java.util.Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private static TopicStatus parseStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return normalized.isBlank() ? null : TopicStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TopicSummary toSummary(TopicEntity topic, List<SupervisorOption> supervisorOptions) {
        List<SupervisorSummary> supervisors = topic.getSupervisors().stream()
                .sorted(Comparator.comparing(UserEntity::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(TopicSupervisorService::toSupervisorSummary)
                .toList();
        return new TopicSummary(
                topic.getId(),
                topic.getTitle(),
                topic.getDepartment().getCode(),
                topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().name(),
                topic.getStatus().name(),
                topic.getProposedBy().getFullName(),
                supervisors,
                supervisors.stream().map(SupervisorSummary::getId).toList(),
                supervisorOptions);
    }

    private static SupervisorSummary toSupervisorSummary(UserEntity user) {
        return new SupervisorSummary(user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode());
    }

    private static SupervisorOption toOption(UserEntity user) {
        return new SupervisorOption(user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getDepartment() == null ? null : user.getDepartment().getId());
    }

    private List<SupervisorOption> supervisorOptionsForDepartment(Long departmentId) {
        return userRepository.findActiveLecturerCapabilitiesOrderByFullName().stream()
                .filter(user -> user.getDepartment() != null
                        && departmentId.equals(user.getDepartment().getId()))
                .map(TopicSupervisorService::toOption)
                .toList();
    }

    private record ManagementScope(boolean isAdmin, Long departmentId, String label) {
        private boolean hasDepartmentScope() {
            return isAdmin || departmentId != null;
        }
    }

    public static final class TopicAssignmentPage {
        private final List<TopicSummary> topics;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final Long departmentId;
        private final Long periodId;
        private final String status;
        private final String sort;
        private final String direction;
        private final String scopeLabel;
        private final List<DepartmentFilterOption> departmentOptions;
        private final List<PeriodFilterOption> periodOptions;
        private final List<String> statusOptions;

        public TopicAssignmentPage(
                List<TopicSummary> topics,
                int page,
                int size,
                int totalItems,
                int totalPages,
                String search,
                Long departmentId,
                Long periodId,
                String status,
                String sort,
                String direction,
                String scopeLabel,
                List<DepartmentFilterOption> departmentOptions,
                List<PeriodFilterOption> periodOptions,
                List<String> statusOptions) {
            this.topics = List.copyOf(topics);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.departmentId = departmentId;
            this.periodId = periodId;
            this.status = status;
            this.sort = sort;
            this.direction = direction;
            this.scopeLabel = scopeLabel;
            this.departmentOptions = List.copyOf(departmentOptions);
            this.periodOptions = List.copyOf(periodOptions);
            this.statusOptions = List.copyOf(statusOptions);
        }

        public List<TopicSummary> getTopics() { return topics; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public Long getDepartmentId() { return departmentId; }
        public Long getPeriodId() { return periodId; }
        public String getStatus() { return status; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
        public String getScopeLabel() { return scopeLabel; }
        public List<DepartmentFilterOption> getDepartmentOptions() { return departmentOptions; }
        public List<PeriodFilterOption> getPeriodOptions() { return periodOptions; }
        public List<String> getStatusOptions() { return statusOptions; }
    }

    public record DepartmentFilterOption(Long id, String code, String name) { }

    public record PeriodFilterOption(Long id, String name, String type) { }

    public static final class TopicSummary {
        private final Long id;
        private final String title;
        private final String departmentCode;
        private final String departmentName;
        private final String periodName;
        private final String periodType;
        private final String status;
        private final String proposedByName;
        private final List<SupervisorSummary> supervisors;
        private final List<Long> supervisorIds;
        private final List<SupervisorOption> supervisorOptions;

        public TopicSummary(Long id, String title, String departmentCode, String departmentName,
                            String periodName, String periodType, String status, String proposedByName,
                            List<SupervisorSummary> supervisors, List<Long> supervisorIds,
                            List<SupervisorOption> supervisorOptions) {
            this.id = id;
            this.title = title;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.periodName = periodName;
            this.periodType = periodType;
            this.status = status;
            this.proposedByName = proposedByName;
            this.supervisors = List.copyOf(supervisors);
            this.supervisorIds = List.copyOf(supervisorIds);
            this.supervisorOptions = List.copyOf(supervisorOptions);
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public String getStatus() { return status; }
        public String getProposedByName() { return proposedByName; }
        public List<SupervisorSummary> getSupervisors() { return supervisors; }
        public List<Long> getSupervisorIds() { return supervisorIds; }
        public List<SupervisorOption> getSupervisorOptions() { return supervisorOptions; }
    }

    public static final class SupervisorSummary {
        private final Long id;
        private final String fullName;
        private final String email;
        private final String departmentCode;

        public SupervisorSummary(Long id, String fullName, String email, String departmentCode) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.departmentCode = departmentCode;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getDepartmentCode() { return departmentCode; }
    }

    public static final class SupervisorOption {
        private final Long id;
        private final String fullName;
        private final String email;
        private final String departmentCode;
        private final String departmentName;
        private final Long departmentId;

        public SupervisorOption(Long id, String fullName, String email, String departmentCode, String departmentName,
                                Long departmentId) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.departmentId = departmentId;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public Long getDepartmentId() { return departmentId; }
    }

    public static class TopicSupervisorNotFoundException extends RuntimeException {
        public TopicSupervisorNotFoundException(Long topicId) {
            super(topicId == null ? "Topic id is required." : "Topic not found: " + topicId);
        }
    }

    public static class TopicSupervisorValidationException extends RuntimeException {
        public TopicSupervisorValidationException(String message) {
            super(message);
        }
    }

    public static class TopicSupervisorAccessException extends RuntimeException {
        public TopicSupervisorAccessException(String message) {
            super(message);
        }
    }
}
