package com.hcmute.topicmanagement.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class EvaluatorAssignmentService {

    private final EvaluationRepository evaluationRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;
    private final UserRepository userRepository;
    private final ReviewBoardMemberRepository reviewBoardMemberRepository;

    public EvaluatorAssignmentService(
            EvaluationRepository evaluationRepository,
            TopicRegistrationRepository topicRegistrationRepository,
            UserRepository userRepository,
            ReviewBoardMemberRepository reviewBoardMemberRepository) {
        this.evaluationRepository = evaluationRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.userRepository = userRepository;
        this.reviewBoardMemberRepository = reviewBoardMemberRepository;
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public EvaluatorAssignmentPage listManageableRegistrations(
            String actorEmail, String search, int page, int size, String sort, String direction) {
        UserEntity actor = findActiveManager(actorEmail);
        ManagementScope scope = scopeFor(actor);
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);

        List<RegistrationSummary> filtered = scope.hasDepartmentScope()
                ? topicRegistrationRepository.findByStatusOrderBySubmittedAtDesc(TopicRegistrationStatus.APPROVED)
                        .stream()
                        .filter(registration -> scope.isAdmin() || belongsToDepartment(scope, registration))
                        .map(this::toSummary)
                        .filter(registration -> matchesSearch(registration, normalizedSearch))
                        .sorted(registrationComparator(normalizedSort, normalizedDirection))
                        .toList()
                : List.of();

        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new EvaluatorAssignmentPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection, scope.label());
    }

    @Transactional
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public RegistrationSummary assignEvaluator(String actorEmail, Long registrationId, Long evaluatorId) {
        if (registrationId == null || registrationId <= 0) {
            throw new EvaluatorAssignmentValidationException("Registration id is invalid.");
        }
        if (evaluatorId == null || evaluatorId <= 0) {
            throw new EvaluatorAssignmentValidationException("Evaluator id is invalid.");
        }

        UserEntity actor = findActiveManager(actorEmail);
        ManagementScope scope = scopeFor(actor);
        TopicRegistrationEntity registration = topicRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new EvaluatorAssignmentNotFoundException(registrationId));
        if (registration.getStatus() != TopicRegistrationStatus.APPROVED) {
            throw new EvaluatorAssignmentValidationException(
                    "Only approved topic registrations can have an evaluator assigned.");
        }
        assertCanManage(scope, registration);

        UserEntity evaluator = findActiveEvaluator(evaluatorId, registration);
        if (isTopicSupervisor(evaluator, registration)) {
            throw new EvaluatorAssignmentValidationException(
                    "A topic supervisor cannot be assigned as that topic's evaluator.");
        }

        EvaluationEntity evaluation;
        if (registration.getReviewBoard() != null) {
            ReviewBoardMemberEntity boardMember = reviewBoardMemberRepository
                    .findByBoard_IdAndLecturer_Id(registration.getReviewBoard().getId(), evaluator.getId())
                    .filter(ReviewBoardMemberEntity::isActive)
                    .orElseThrow(() -> new EvaluatorAssignmentValidationException(
                            "This registration is managed by its review board; select an active board member."));
            evaluation = evaluationRepository
                    .findByTopicRegistration_IdAndLecturer_IdAndBoard_Id(
                            registrationId, evaluator.getId(), registration.getReviewBoard().getId())
                    .or(() -> evaluationRepository.findFirstByTopicRegistration_IdAndBoard_IdOrderByCreatedAtAsc(
                            registrationId, registration.getReviewBoard().getId()))
                    .orElseGet(() -> new EvaluationEntity(registration, evaluator));
            evaluation.setBoard(registration.getReviewBoard());
            evaluation.setBoardMember(boardMember);
        } else {
            evaluation = evaluationRepository
                    .findByTopicRegistration_IdAndLecturer_Id(registrationId, evaluator.getId())
                    .or(() -> evaluationRepository.findFirstByTopicRegistration_IdOrderByCreatedAtAsc(registrationId))
                    .orElseGet(() -> new EvaluationEntity(registration, evaluator));
        }
        evaluation.setLecturer(evaluator);
        EvaluationEntity savedEvaluation = evaluationRepository.saveAndFlush(evaluation);
        evaluationRepository.touchUpdatedAt(savedEvaluation.getId());
        return toSummary(savedEvaluation);
    }

    private UserEntity findActiveManager(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> hasActiveRole(user, "ADMIN") || hasActiveRole(user, "FACULTY_HEAD"))
                .orElseThrow(() -> new EvaluatorAssignmentAccessException(
                        "Only an active Admin or Faculty Head can assign evaluators."));
    }

    private UserEntity findActiveEvaluator(Long evaluatorId, TopicRegistrationEntity registration) {
        List<UserEntity> candidates = userRepository.findActiveLecturerCapabilitiesByIdIn(List.of(evaluatorId));
        if (candidates.size() != 1) {
            throw new EvaluatorAssignmentValidationException(
                    "Evaluator must be an active Lecturer or Faculty Head.");
        }
        UserEntity evaluator = candidates.get(0);
        Long topicDepartmentId = registration.getTopic().getDepartment() == null
                ? null : registration.getTopic().getDepartment().getId();
        if (topicDepartmentId == null
                || evaluator.getDepartment() == null
                || !Objects.equals(topicDepartmentId, evaluator.getDepartment().getId())) {
            throw new EvaluatorAssignmentValidationException(
                    "Evaluator must belong to the topic's department.");
        }
        return evaluator;
    }

    private RegistrationSummary toSummary(TopicRegistrationEntity registration) {
        EvaluationEntity evaluation = registration.getReviewBoard() == null
                ? evaluationRepository.findFirstByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId())
                        .orElse(null)
                : evaluationRepository.findFirstByTopicRegistration_IdAndBoard_IdOrderByUpdatedAtDesc(
                        registration.getId(), registration.getReviewBoard().getId()).orElse(null);
        Long topicDepartmentId = registration.getTopic().getDepartment() == null
                ? null : registration.getTopic().getDepartment().getId();
        List<EvaluatorOption> options;
        if (topicDepartmentId == null) {
            options = List.of();
        } else if (registration.getReviewBoard() != null) {
            options = reviewBoardMemberRepository
                    .findByBoard_IdAndActiveTrueOrderByMemberRoleAscAssignedAtAsc(
                            registration.getReviewBoard().getId())
                    .stream()
                    .map(ReviewBoardMemberEntity::getLecturer)
                    .filter(candidate -> !isTopicSupervisor(candidate, registration))
                    .map(EvaluatorAssignmentService::toOption)
                    .toList();
        } else {
            options = userRepository
                    .findActiveLecturerCapabilitiesByDepartmentIdOrderByFullName(topicDepartmentId).stream()
                    .filter(candidate -> !isTopicSupervisor(candidate, registration))
                    .map(EvaluatorAssignmentService::toOption)
                    .toList();
        }
        return new RegistrationSummary(
                registration.getId(),
                registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                registration.getSubmittedAt(),
                evaluation == null ? null : toEvaluator(evaluation.getLecturer()),
                options);
    }

    private static RegistrationSummary toSummary(EvaluationEntity evaluation) {
        TopicRegistrationEntity registration = evaluation.getTopicRegistration();
        return new RegistrationSummary(
                registration.getId(),
                registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                registration.getSubmittedAt(),
                toEvaluator(evaluation.getLecturer()),
                List.of());
    }

    private static EvaluatorSummary toEvaluator(UserEntity user) {
        return user == null ? null : new EvaluatorSummary(
                user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode());
    }

    private static EvaluatorOption toOption(UserEntity user) {
        return new EvaluatorOption(
                user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private static ManagementScope scopeFor(UserEntity actor) {
        if (hasActiveRole(actor, "ADMIN")) {
            return new ManagementScope(true, null, "Admin · all departments");
        }
        DepartmentEntity department = actor.getDepartment();
        if (department == null || department.getId() == null) {
            return new ManagementScope(false, null, "Faculty Head · no department assigned");
        }
        return new ManagementScope(false, department.getId(),
                "Faculty Head · " + department.getCode() + " · " + department.getName());
    }

    private static boolean belongsToDepartment(ManagementScope scope, TopicRegistrationEntity registration) {
        return registration.getTopic().getDepartment() != null
                && scope.departmentId() != null
                && Objects.equals(scope.departmentId(), registration.getTopic().getDepartment().getId());
    }

    private static void assertCanManage(ManagementScope scope, TopicRegistrationEntity registration) {
        if (!scope.isAdmin() && !belongsToDepartment(scope, registration)) {
            throw new EvaluatorAssignmentAccessException(
                    "Faculty Heads can only assign evaluators for their department.");
        }
        if (!scope.hasDepartmentScope()) {
            throw new EvaluatorAssignmentAccessException(
                    "The Faculty Head has no department assignment.");
        }
    }

    private static boolean isTopicSupervisor(UserEntity evaluator, TopicRegistrationEntity registration) {
        return evaluator.getId() != null && registration.getTopic().getSupervisors().stream()
                .anyMatch(supervisor -> Objects.equals(evaluator.getId(), supervisor.getId()));
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.isActive())
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean matchesSearch(RegistrationSummary registration, String search) {
        return search.isBlank()
                || contains(registration.groupName(), search)
                || contains(registration.topicTitle(), search)
                || contains(registration.departmentCode(), search)
                || contains(registration.departmentName(), search)
                || contains(registration.periodName(), search)
                || contains(registration.assignedEvaluator() == null
                        ? null : registration.assignedEvaluator().fullName(), search);
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<RegistrationSummary> registrationComparator(String sort, String direction) {
        Comparator<RegistrationSummary> comparator = switch (sort) {
            case "group" -> Comparator.comparing(RegistrationSummary::groupName, String.CASE_INSENSITIVE_ORDER);
            case "department" -> Comparator.comparing(
                    RegistrationSummary::departmentCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "period" -> Comparator.comparing(
                    RegistrationSummary::periodName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "evaluator" -> Comparator.comparing(
                    registration -> registration.assignedEvaluator() == null
                            ? "" : registration.assignedEvaluator().fullName(),
                    String.CASE_INSENSITIVE_ORDER);
            case "submitted" -> Comparator.comparing(
                    RegistrationSummary::submittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(RegistrationSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "group", "department", "period", "evaluator", "submitted" ->
                    sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private record ManagementScope(boolean isAdmin, Long departmentId, String label) {
        private boolean hasDepartmentScope() {
            return isAdmin || departmentId != null;
        }
    }

    public record EvaluatorAssignmentPage(
            List<RegistrationSummary> registrations,
            int page,
            int size,
            int totalItems,
            int totalPages,
            String search,
            String sort,
            String direction,
            String scopeLabel) {
        public EvaluatorAssignmentPage {
            registrations = List.copyOf(registrations);
        }

        public boolean hasPrevious() { return page > 0; }
        public boolean hasNext() { return page + 1 < totalPages; }
    }

    public record RegistrationSummary(
            Long id,
            Long groupId,
            String groupName,
            Long topicId,
            String topicTitle,
            String departmentCode,
            String departmentName,
            Long periodId,
            String periodName,
            java.time.LocalDateTime submittedAt,
            EvaluatorSummary assignedEvaluator,
            List<EvaluatorOption> evaluatorOptions) {
        public RegistrationSummary {
            evaluatorOptions = List.copyOf(evaluatorOptions);
        }
    }

    public record EvaluatorSummary(Long id, String fullName, String email, String departmentCode) {
    }

    public record EvaluatorOption(
            Long id, String fullName, String email, String departmentCode, String departmentName) {
    }

    public static class EvaluatorAssignmentNotFoundException extends RuntimeException {
        public EvaluatorAssignmentNotFoundException(Long registrationId) {
            super(registrationId == null
                    ? "Registration id is required."
                    : "Topic registration not found: " + registrationId);
        }
    }

    public static class EvaluatorAssignmentValidationException extends RuntimeException {
        public EvaluatorAssignmentValidationException(String message) {
            super(message);
        }
    }

    public static class EvaluatorAssignmentAccessException extends RuntimeException {
        public EvaluatorAssignmentAccessException(String message) {
            super(message);
        }
    }
}
