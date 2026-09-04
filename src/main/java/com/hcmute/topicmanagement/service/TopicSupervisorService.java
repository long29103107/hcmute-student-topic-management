package com.hcmute.topicmanagement.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
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
        UserEntity actor = findActiveActor(actorEmail);
        ManagementScope scope = scopeFor(actor);
        if (!scope.hasDepartmentScope()) {
            return new TopicAssignmentPage(List.of(), List.of(), scope.label());
        }

        List<TopicEntity> topics = scope.isAdmin()
                ? topicRepository.findAllForSupervisorManagement()
                : topicRepository.findForSupervisorManagementByDepartmentId(scope.departmentId());
        List<SupervisorOption> options = userRepository.findActiveLecturerCapabilitiesOrderByFullName().stream()
                .map(TopicSupervisorService::toOption)
                .toList();
        return new TopicAssignmentPage(
                topics.stream().map(TopicSupervisorService::toSummary).toList(), options, scope.label());
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

        topic.getSupervisors().clear();
        normalizedIds.forEach(id -> topic.getSupervisors().add(candidatesById.get(id)));
        return toSummary(topicRepository.saveAndFlush(topic));
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

    private static TopicSummary toSummary(TopicEntity topic) {
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
                supervisors.stream().map(SupervisorSummary::getId).toList());
    }

    private static SupervisorSummary toSupervisorSummary(UserEntity user) {
        return new SupervisorSummary(user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode());
    }

    private static SupervisorOption toOption(UserEntity user) {
        return new SupervisorOption(user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private record ManagementScope(boolean isAdmin, Long departmentId, String label) {
        private boolean hasDepartmentScope() {
            return isAdmin || departmentId != null;
        }
    }

    public static final class TopicAssignmentPage {
        private final List<TopicSummary> topics;
        private final List<SupervisorOption> supervisorOptions;
        private final String scopeLabel;

        public TopicAssignmentPage(List<TopicSummary> topics, List<SupervisorOption> supervisorOptions,
                                   String scopeLabel) {
            this.topics = List.copyOf(topics);
            this.supervisorOptions = List.copyOf(supervisorOptions);
            this.scopeLabel = scopeLabel;
        }

        public List<TopicSummary> getTopics() { return topics; }
        public List<SupervisorOption> getSupervisorOptions() { return supervisorOptions; }
        public String getScopeLabel() { return scopeLabel; }
    }

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

        public TopicSummary(Long id, String title, String departmentCode, String departmentName,
                            String periodName, String periodType, String status, String proposedByName,
                            List<SupervisorSummary> supervisors, List<Long> supervisorIds) {
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

        public SupervisorOption(Long id, String fullName, String email, String departmentCode, String departmentName) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
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
