package com.hcmute.topicmanagement.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReportRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final DepartmentRepository departmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final ReportRepository reportRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public DashboardService(
            DepartmentRepository departmentRepository,
            EvaluationRepository evaluationRepository,
            RegistrationPeriodRepository registrationPeriodRepository,
            RegistrationResultRepository registrationResultRepository,
            ReportRepository reportRepository,
            StudentGroupRepository studentGroupRepository,
            TopicRegistrationRepository topicRegistrationRepository,
            TopicRepository topicRepository,
            UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.reportRepository = reportRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    public DashboardView load(String email) {
        UserEntity actor = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new IllegalStateException("Dashboard account is unavailable."));

        if (hasActiveRole(actor, "ADMIN")) {
            return new DashboardView("admin", buildAdmin(actor));
        }
        if (hasActiveRole(actor, "FACULTY_HEAD")) {
            return new DashboardView("faculty-head", buildFacultyHead(actor));
        }
        if (hasActiveRole(actor, "LECTURER")) {
            return new DashboardView("lecturer", buildLecturer(actor));
        }
        return new DashboardView("student", buildStudent(actor));
    }

    private DashboardData buildAdmin(UserEntity actor) {
        List<UserEntity> users = userRepository.findAll();
        List<RegistrationPeriodEntity> openPeriods = openPeriods();
        return new DashboardData(
                actor.getFullName(),
                "Administrator",
                departmentLabel(actor),
                periodName(openPeriods),
                periodStatus(openPeriods),
                users.size(),
                users.stream().filter(UserEntity::isActive).count(),
                departmentRepository.count(),
                openPeriods.size(),
                topicRepository.findByStatusForReview(TopicStatus.PENDING_APPROVAL).size(),
                topicRegistrationRepository.findByStatusForReview(TopicRegistrationStatus.PENDING).size(),
                evaluationRepository.findAll().stream()
                        .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.DRAFT)
                        .count(),
                0,
                registrationResultRepository.findByStatusOrderByUpdatedAtDesc(RegistrationResultStatus.PUBLISHED).size(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false);
    }

    private DashboardData buildFacultyHead(UserEntity actor) {
        Long departmentId = actor.getDepartment() == null ? null : actor.getDepartment().getId();
        List<RegistrationPeriodEntity> openPeriods = openPeriods();
        List<TopicEntity> topics = departmentId == null
                ? List.of()
                : topicRepository.findForSupervisorManagementByDepartmentId(departmentId);
        List<TopicRegistrationEntity> pendingRegistrations = topicRegistrationRepository
                .findByStatusForReview(TopicRegistrationStatus.PENDING).stream()
                .filter(registration -> belongsToDepartment(registration, departmentId))
                .toList();
        List<TopicRegistrationEntity> approvedRegistrations = topicRegistrationRepository
                .findByStatusForReview(TopicRegistrationStatus.APPROVED).stream()
                .filter(registration -> belongsToDepartment(registration, departmentId))
                .toList();
        List<EvaluationEntity> evaluations = evaluationRepository.findAll().stream()
                .filter(evaluation -> belongsToDepartment(evaluation.getTopicRegistration(), departmentId))
                .toList();
        long unassignedEvaluations = approvedRegistrations.stream()
                .filter(registration -> evaluationRepository
                        .findFirstByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId()).isEmpty())
                .count();
        long publishableResults = approvedRegistrations.stream()
                .filter(this::isPublishable)
                .count();
        long publishedResults = registrationResultRepository
                .findByStatusOrderByUpdatedAtDesc(RegistrationResultStatus.PUBLISHED).stream()
                .filter(result -> belongsToDepartment(result.getTopicRegistration(), departmentId))
                .count();

        return new DashboardData(
                actor.getFullName(),
                "Faculty Head",
                departmentLabel(actor),
                periodName(openPeriods),
                periodStatus(openPeriods),
                0,
                0,
                0,
                openPeriods.size(),
                topics.stream().filter(topic -> topic.getStatus() == TopicStatus.PENDING_APPROVAL).count(),
                pendingRegistrations.size(),
                evaluations.stream().filter(evaluation -> evaluation.getStatus() == EvaluationStatus.DRAFT).count(),
                publishableResults,
                publishedResults,
                0,
                0,
                0,
                0,
                0,
                unassignedEvaluations,
                approvedRegistrations.size(),
                0,
                0,
                false);
    }

    private DashboardData buildLecturer(UserEntity actor) {
        List<RegistrationPeriodEntity> openPeriods = openPeriods();
        List<TopicEntity> ownTopics = topicRepository.findOwnProposalsWithPeriodAndDepartment(actor.getId());
        List<EvaluationEntity> assignedEvaluations = evaluationRepository
                .findByLecturer_IdOrderByUpdatedAtDesc(actor.getId()).stream()
                .filter(evaluation -> evaluation.getTopicRegistration() != null
                        && evaluation.getTopicRegistration().getStatus() == TopicRegistrationStatus.APPROVED)
                .toList();
        return new DashboardData(
                actor.getFullName(),
                "Lecturer",
                departmentLabel(actor),
                periodName(openPeriods),
                periodStatus(openPeriods),
                0,
                0,
                0,
                openPeriods.size(),
                0,
                0,
                assignedEvaluations.stream().filter(evaluation -> evaluation.getStatus() == EvaluationStatus.DRAFT).count(),
                0,
                0,
                ownTopics.size(),
                ownTopics.stream().filter(topic -> topic.getStatus() == TopicStatus.PENDING_APPROVAL).count(),
                assignedEvaluations.size(),
                assignedEvaluations.stream().filter(evaluation -> evaluation.getStatus() == EvaluationStatus.DRAFT).count(),
                0,
                0,
                0,
                0,
                0,
                false);
    }

    private DashboardData buildStudent(UserEntity actor) {
        List<RegistrationPeriodEntity> openPeriods = openPeriods();
        List<StudentGroupEntity> groups = studentGroupRepository.findRelatedGroupsWithDetails(actor.getId());
        List<TopicRegistrationEntity> registrations = topicRegistrationRepository.findForStudentWithDetails(actor.getId());
        Set<Long> registrationIds = registrations.stream()
                .map(TopicRegistrationEntity::getId)
                .collect(Collectors.toSet());
        long submittedReports = registrations.stream()
                .filter(registration -> registration.getStatus() == TopicRegistrationStatus.APPROVED)
                .filter(registration -> reportRepository
                        .findFirstByTopicRegistration_IdOrderBySubmittedAtDesc(registration.getId()).isPresent())
                .count();
        long publishedResults = registrationResultRepository
                .findByStatusOrderByUpdatedAtDesc(RegistrationResultStatus.PUBLISHED).stream()
                .filter(result -> registrationIds.contains(result.getRegistrationId()))
                .count();
        boolean canRegisterTopic = groups.stream()
                .anyMatch(group -> group.getStatus() == GroupStatus.ACTIVE
                        && group.getLeader() != null
                        && Objects.equals(group.getLeader().getId(), actor.getId()));

        return new DashboardData(
                actor.getFullName(),
                "Student",
                departmentLabel(actor),
                periodName(openPeriods),
                periodStatus(openPeriods),
                0,
                0,
                0,
                openPeriods.size(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                groups.stream().filter(group -> group.getStatus() == GroupStatus.ACTIVE).count(),
                registrations.stream().filter(registration -> registration.getStatus() == TopicRegistrationStatus.PENDING).count(),
                registrations.stream().filter(registration -> registration.getStatus() == TopicRegistrationStatus.APPROVED).count(),
                submittedReports,
                publishedResults,
                canRegisterTopic);
    }

    private boolean isPublishable(TopicRegistrationEntity registration) {
        if (registrationResultRepository.findByTopicRegistration_Id(registration.getId())
                .map(result -> result.getStatus() == RegistrationResultStatus.PUBLISHED)
                .orElse(false)) {
            return false;
        }
        List<EvaluationEntity> evaluations = evaluationRepository
                .findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId());
        return !evaluations.isEmpty() && evaluations.stream().allMatch(this::hasSubmittedScore);
    }

    private boolean hasSubmittedScore(EvaluationEntity evaluation) {
        return evaluation.getScore() != null
                && (evaluation.getStatus() == EvaluationStatus.SUBMITTED
                        || evaluation.getStatus() == EvaluationStatus.PUBLISHED);
    }

    private List<RegistrationPeriodEntity> openPeriods() {
        return registrationPeriodRepository
                .findByStatusOrderByStudentRegistrationStartDesc(RegistrationPeriodStatus.OPEN);
    }

    private static boolean belongsToDepartment(TopicRegistrationEntity registration, Long departmentId) {
        return registration != null
                && registration.getTopic() != null
                && registration.getTopic().getDepartment() != null
                && departmentId != null
                && Objects.equals(registration.getTopic().getDepartment().getId(), departmentId);
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive() && roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static String departmentLabel(UserEntity actor) {
        DepartmentEntity department = actor.getDepartment();
        return department == null ? "All departments" : department.getCode() + " · " + department.getName();
    }

    private static String periodName(List<RegistrationPeriodEntity> periods) {
        return periods.isEmpty() ? "No open registration period" : periods.get(0).getName();
    }

    private static String periodStatus(List<RegistrationPeriodEntity> periods) {
        return periods.isEmpty() ? "Unavailable" : periods.get(0).getStatus().name();
    }

    public record DashboardView(String template, DashboardData data) {
    }

    public record DashboardData(
            String displayName,
            String roleLabel,
            String departmentLabel,
            String activePeriodName,
            String activePeriodStatus,
            long totalUsers,
            long activeUsers,
            long departmentCount,
            long openPeriodCount,
            long pendingTopicReviews,
            long pendingRegistrationReviews,
            long pendingEvaluations,
            long publishableResults,
            long publishedResults,
            long ownTopicCount,
            long pendingOwnTopics,
            long assignedEvaluationCount,
            long pendingAssignedEvaluations,
            long activeGroupCount,
            long pendingRegistrationCount,
            long approvedRegistrationCount,
            long submittedReportCount,
            long visibleResultCount,
            boolean canRegisterTopic) {
    }
}
