package com.hcmute.topicmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ResultPublicationService {

    private final TopicRegistrationRepository topicRegistrationRepository;
    private final EvaluationRepository evaluationRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final UserRepository userRepository;

    public ResultPublicationService(
            TopicRegistrationRepository topicRegistrationRepository,
            EvaluationRepository evaluationRepository,
            RegistrationResultRepository registrationResultRepository,
            UserRepository userRepository) {
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public PublicationPage listForPublication(String publisherEmail) {
        UserEntity publisher = findActivePublisher(publisherEmail);
        List<ResultSummary> results = topicRegistrationRepository
                .findByStatusForReview(TopicRegistrationStatus.APPROVED).stream()
                .filter(registration -> canManage(publisher, registration))
                .map(this::toSummary)
                .toList();
        return new PublicationPage(results, scopeLabel(publisher));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public ResultSummary publish(String publisherEmail, Long registrationId) {
        if (registrationId == null || registrationId <= 0) {
            throw new ResultPublicationValidationException("Registration id is invalid.");
        }
        UserEntity publisher = findActivePublisher(publisherEmail);
        TopicRegistrationEntity registration = topicRegistrationRepository
                .findApprovedByIdForReadOnly(registrationId)
                .orElseThrow(() -> new ResultPublicationNotFoundException(registrationId));
        if (!canManage(publisher, registration)) {
            throw new ResultPublicationAccessException(
                    "Faculty Heads can only publish results for their department.");
        }

        List<EvaluationEntity> evaluations = evaluationRepository
                .findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId());
        if (evaluations.isEmpty()) {
            throw new ResultPublicationValidationException(
                    "A result cannot be published until an evaluator has been assigned.");
        }
        if (evaluations.stream().anyMatch(evaluation -> !isSubmittedWithScore(evaluation))) {
            throw new ResultPublicationValidationException(
                    "A result can be published only after every assigned evaluator submits a score.");
        }

        RegistrationResultEntity result = registrationResultRepository
                .findByTopicRegistration_Id(registration.getId())
                .orElseGet(() -> new RegistrationResultEntity(registration));
        if (result.getStatus() == RegistrationResultStatus.PUBLISHED) {
            throw new ResultPublicationValidationException(
                    "Published results cannot be changed without an audited action.");
        }

        LocalDateTime now = LocalDateTime.now();
        result.setAverageScore(averageScore(evaluations));
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        result.setFinalizedBy(publisher);
        result.setFinalizedAt(now);
        result.setPublishedBy(publisher);
        result.setPublishedAt(now);
        registrationResultRepository.saveAndFlush(result);
        return toSummary(registration);
    }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('RESULT_VIEW')")
    public List<StudentResultSummary> listForStudent(String studentEmail) {
        UserEntity student = findActiveStudent(studentEmail);
        return topicRegistrationRepository.findForStudentWithDetails(student.getId()).stream()
                .filter(registration -> registration.getStatus() == TopicRegistrationStatus.APPROVED)
                .map(registration -> registrationResultRepository
                        .findByTopicRegistration_Id(registration.getId())
                        .filter(result -> result.getStatus() == RegistrationResultStatus.PUBLISHED)
                        .map(result -> toStudentSummary(registration, result)))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private ResultSummary toSummary(TopicRegistrationEntity registration) {
        List<EvaluationEntity> evaluations = evaluationRepository
                .findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId());
        RegistrationResultEntity result = registrationResultRepository
                .findByTopicRegistration_Id(registration.getId()).orElse(null);
        long submittedCount = evaluations.stream().filter(this::isSubmittedWithScore).count();
        boolean published = result != null && result.getStatus() == RegistrationResultStatus.PUBLISHED;
        BigDecimal average = submittedCount == evaluations.size() && !evaluations.isEmpty()
                ? averageScore(evaluations)
                : null;
        return new ResultSummary(
                registration.getId(),
                registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                evaluations.size(),
                (int) submittedCount,
                result == null ? "NOT_CREATED" : result.getStatus().name(),
                result != null && result.getAverageScore() != null ? result.getAverageScore() : average,
                result == null ? null : result.getFinalComment(),
                result == null ? null : result.getPublishedAt(),
                !published && submittedCount == evaluations.size() && !evaluations.isEmpty());
    }

    private static StudentResultSummary toStudentSummary(
            TopicRegistrationEntity registration, RegistrationResultEntity result) {
        StudentGroupEntity group = registration.getStudentGroup();
        return new StudentResultSummary(
                registration.getId(),
                group.getId(),
                group.getName(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                result.getAverageScore(),
                result.getFinalComment(),
                result.getPublishedAt());
    }

    private BigDecimal averageScore(List<EvaluationEntity> evaluations) {
        BigDecimal total = evaluations.stream()
                .filter(this::isSubmittedWithScore)
                .map(EvaluationEntity::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = evaluations.stream().filter(this::isSubmittedWithScore).count();
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private boolean isSubmittedWithScore(EvaluationEntity evaluation) {
        return evaluation.getScore() != null
                && (evaluation.getStatus() == EvaluationStatus.SUBMITTED
                        || evaluation.getStatus() == EvaluationStatus.PUBLISHED);
    }

    private UserEntity findActivePublisher(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> hasActiveRole(user, "ADMIN") || hasActiveRole(user, "FACULTY_HEAD"))
                .orElseThrow(() -> new ResultPublicationAccessException(
                        "Only an active Admin or Faculty Head can publish results."));
    }

    private UserEntity findActiveStudent(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> hasActiveRole(user, "STUDENT"))
                .orElseThrow(() -> new ResultPublicationAccessException(
                        "Only an active Student can view group results."));
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive()
                        && roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean canManage(UserEntity publisher, TopicRegistrationEntity registration) {
        if (hasActiveRole(publisher, "ADMIN")) {
            return true;
        }
        DepartmentEntity publisherDepartment = publisher.getDepartment();
        DepartmentEntity topicDepartment = registration.getTopic().getDepartment();
        return publisherDepartment != null && topicDepartment != null
                && Objects.equals(publisherDepartment.getId(), topicDepartment.getId());
    }

    private static String scopeLabel(UserEntity publisher) {
        if (hasActiveRole(publisher, "ADMIN")) {
            return "Administrator · all departments";
        }
        return publisher.getDepartment() == null
                ? "Faculty Head · department not assigned"
                : "Faculty Head · " + publisher.getDepartment().getCode();
    }

    public record PublicationPage(List<ResultSummary> results, String scopeLabel) {
        public PublicationPage {
            results = List.copyOf(results);
        }
    }

    public record ResultSummary(
            Long registrationId,
            Long groupId,
            String groupName,
            Long topicId,
            String topicTitle,
            String departmentCode,
            String departmentName,
            Long periodId,
            String periodName,
            int evaluationCount,
            int submittedEvaluationCount,
            String status,
            BigDecimal averageScore,
            String finalComment,
            LocalDateTime publishedAt,
            boolean publishable) {
    }

    public record StudentResultSummary(
            Long registrationId,
            Long groupId,
            String groupName,
            Long topicId,
            String topicTitle,
            String departmentCode,
            String departmentName,
            Long periodId,
            String periodName,
            BigDecimal averageScore,
            String finalComment,
            LocalDateTime publishedAt) {
    }

    public static class ResultPublicationNotFoundException extends RuntimeException {
        public ResultPublicationNotFoundException(Long registrationId) {
            super(registrationId == null
                    ? "Registration id is required."
                    : "Approved topic registration not found: " + registrationId);
        }
    }

    public static class ResultPublicationValidationException extends RuntimeException {
        public ResultPublicationValidationException(String message) {
            super(message);
        }
    }

    public static class ResultPublicationAccessException extends RuntimeException {
        public ResultPublicationAccessException(String message) {
            super(message);
        }
    }
}
