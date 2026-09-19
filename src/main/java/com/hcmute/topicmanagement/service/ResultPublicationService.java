package com.hcmute.topicmanagement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ResultPublicationService {

    private final TopicRegistrationRepository topicRegistrationRepository;
    private final EvaluationRepository evaluationRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final ReviewBoardRepository reviewBoardRepository;
    private final ReviewBoardMemberRepository reviewBoardMemberRepository;
    private final UserRepository userRepository;
    private final EvaluationScoreCalculator scoreCalculator;

    public ResultPublicationService(
            TopicRegistrationRepository topicRegistrationRepository,
            EvaluationRepository evaluationRepository,
            RegistrationResultRepository registrationResultRepository,
            ReviewBoardRepository reviewBoardRepository,
            ReviewBoardMemberRepository reviewBoardMemberRepository,
            UserRepository userRepository,
            EvaluationScoreCalculator scoreCalculator) {
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.reviewBoardRepository = reviewBoardRepository;
        this.reviewBoardMemberRepository = reviewBoardMemberRepository;
        this.userRepository = userRepository;
        this.scoreCalculator = scoreCalculator;
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public PublicationPage listForPublication(String publisherEmail) {
        return listForPublication(publisherEmail, "");
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public PublicationPage listForPublication(String publisherEmail, String search) {
        UserEntity publisher = findActivePublisher(publisherEmail);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<ResultSummary> results = topicRegistrationRepository
                .findByStatusForReview(TopicRegistrationStatus.APPROVED).stream()
                .filter(registration -> canManage(publisher, registration))
                .map(this::toSummary)
                .filter(result -> normalizedSearch.isEmpty() || matchesSearch(result, normalizedSearch))
                .toList();
        return new PublicationPage(results, scopeLabel(publisher));
    }

    private boolean matchesSearch(ResultSummary result, String search) {
        return contains(result.groupName(), search)
                || contains(result.topicTitle(), search)
                || contains(result.departmentCode(), search)
                || contains(result.departmentName(), search)
                || contains(result.periodName(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
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
        RegistrationResultEntity result = registrationResultRepository
                .findByTopicRegistration_Id(registration.getId())
                .orElseGet(() -> new RegistrationResultEntity(registration));
        if (result.getStatus() == RegistrationResultStatus.PUBLISHED) {
            throw new ResultPublicationValidationException(
                    "Published results cannot be changed without an audited action.");
        }
        EvaluationContext context = evaluationContext(evaluations);
        if (context.requiredEvaluations().isEmpty()) {
            throw new ResultPublicationValidationException(
                    "A result cannot be published until an evaluator has been assigned.");
        }
        if (!context.structurallyValid()) {
            throw new ResultPublicationValidationException(
                    "Review board evaluations must be assigned to every active board member.");
        }
        if (context.board() != null
                && context.board().getStatus() != ReviewBoardStatus.COMPLETED
                && context.board().getStatus() != ReviewBoardStatus.PUBLISHED) {
            throw new ResultPublicationValidationException(
                    "A review board must be completed before its result can be published.");
        }
        if (context.requiredEvaluations().stream()
                .anyMatch(evaluation -> !scoreCalculator.isSubmittedWithValidScore(evaluation))) {
            throw new ResultPublicationValidationException(
                    "A result can be published only after every assigned evaluator submits a score.");
        }

        LocalDateTime now = LocalDateTime.now();
        result.setAverageScore(scoreCalculator.averageScore(context.requiredEvaluations()));
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        result.setFinalizedBy(publisher);
        result.setFinalizedAt(now);
        result.setPublishedBy(publisher);
        result.setPublishedAt(now);
        registrationResultRepository.saveAndFlush(result);
        if (context.board() != null) {
            context.board().setStatus(ReviewBoardStatus.PUBLISHED);
            reviewBoardRepository.saveAndFlush(context.board());
        }
        return toSummary(registration);
    }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('RESULT_VIEW')")
    public List<StudentResultSummary> listForStudent(String studentEmail) {
        UserEntity student = findActiveStudent(studentEmail);
        return listPublishedStudentResults(student.getId());
        }

        @PreAuthorize("hasRole('STUDENT') and hasAuthority('RESULT_VIEW')")
        public StudentResultPage listForStudentPage(
            String studentEmail, String search, Long departmentId, Long periodId,
            int page, int size, String sort, String direction) {
        UserEntity student = findActiveStudent(studentEmail);
        List<StudentResultSummary> allResults = listPublishedStudentResults(student.getId());
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        List<StudentResultSummary> filtered = allResults.stream()
            .filter(result -> departmentId == null || departmentId.equals(result.departmentId()))
            .filter(result -> periodId == null || periodId.equals(result.periodId()))
            .filter(result -> matchesStudentSearch(result, normalizedSearch))
            .sorted(studentResultComparator(normalizedSort, normalizedDirection))
            .toList();
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        List<FilterOption> departments = allResults.stream()
            .filter(result -> result.departmentId() != null)
            .map(result -> new FilterOption(result.departmentId(), result.departmentCode(), result.departmentName()))
            .distinct()
            .sorted(Comparator.comparing(FilterOption::label, String.CASE_INSENSITIVE_ORDER))
            .toList();
        List<FilterOption> periods = allResults.stream()
            .filter(result -> result.periodId() != null)
            .map(result -> new FilterOption(result.periodId(), result.periodName(), ""))
            .distinct()
            .sorted(Comparator.comparing(FilterOption::label, String.CASE_INSENSITIVE_ORDER))
            .toList();
        return new StudentResultPage(
            filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
            normalizedSearch, normalizedSort, normalizedDirection, departmentId, periodId,
            departments, periods);
        }

        private List<StudentResultSummary> listPublishedStudentResults(Long studentId) {
        return topicRegistrationRepository.findForStudentWithDetails(studentId).stream()
                .filter(registration -> registration.getStatus() == TopicRegistrationStatus.APPROVED)
                .map(registration -> registrationResultRepository
                        .findByTopicRegistration_Id(registration.getId())
                        .filter(result -> result.getStatus() == RegistrationResultStatus.PUBLISHED)
                        .map(result -> toStudentSummary(registration, result)))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private static boolean matchesStudentSearch(StudentResultSummary result, String search) {
        return search.isBlank()
                || containsStudentValue(result.groupName(), search)
                || containsStudentValue(result.topicTitle(), search)
                || containsStudentValue(result.departmentCode(), search)
                || containsStudentValue(result.departmentName(), search)
                || containsStudentValue(result.periodName(), search)
                || containsStudentValue(result.finalComment(), search)
                || containsStudentValue(result.averageScore() == null ? null : result.averageScore().toPlainString(), search);
    }

    private static boolean containsStudentValue(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<StudentResultSummary> studentResultComparator(String sort, String direction) {
        Comparator<StudentResultSummary> comparator = switch (sort) {
            case "group" -> Comparator.comparing(
                    StudentResultSummary::groupName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
            case "department" -> Comparator.comparing(
                    StudentResultSummary::departmentCode, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    StudentResultSummary::periodName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
            case "score" -> Comparator.comparing(
                    StudentResultSummary::averageScore, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
            case "published" -> Comparator.comparing(
                    StudentResultSummary::publishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    StudentResultSummary::topicTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(StudentResultSummary::groupName, String.CASE_INSENSITIVE_ORDER);
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "group", "department", "period", "score", "published" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private ResultSummary toSummary(TopicRegistrationEntity registration) {
        List<EvaluationEntity> evaluations = evaluationRepository
                .findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId());
        EvaluationContext context = evaluationContext(evaluations);
        RegistrationResultEntity result = registrationResultRepository
                .findByTopicRegistration_Id(registration.getId()).orElse(null);
        List<EvaluationEntity> requiredEvaluations = context.requiredEvaluations();
        long submittedCount = requiredEvaluations.stream()
                .filter(scoreCalculator::isSubmittedWithValidScore)
                .count();
        boolean published = result != null && result.getStatus() == RegistrationResultStatus.PUBLISHED;
        BigDecimal average = submittedCount == requiredEvaluations.size() && !requiredEvaluations.isEmpty()
                ? scoreCalculator.averageScore(requiredEvaluations)
                : null;
        boolean boardCompleted = context.board() == null
                || context.board().getStatus() == ReviewBoardStatus.COMPLETED
                || context.board().getStatus() == ReviewBoardStatus.PUBLISHED;
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
                requiredEvaluations.size(),
                (int) submittedCount,
                result == null ? "NOT_CREATED" : result.getStatus().name(),
                result != null && result.getAverageScore() != null ? result.getAverageScore() : average,
                result == null ? null : result.getFinalComment(),
                result == null ? null : result.getPublishedAt(),
                !published && context.structurallyValid() && boardCompleted
                        && submittedCount == requiredEvaluations.size() && !requiredEvaluations.isEmpty());
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
                registration.getTopic().getDepartment().getId(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                result.getAverageScore(),
                result.getFinalComment(),
                result.getPublishedAt());
    }

    private EvaluationContext evaluationContext(List<EvaluationEntity> evaluations) {
        ReviewBoardEntity board = evaluations.stream()
                .map(EvaluationEntity::getBoard)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (board == null) {
            return new EvaluationContext(evaluations, null, true);
        }

        boolean structurallyValid = evaluations.stream().allMatch(evaluation ->
                evaluation.getBoard() != null && Objects.equals(evaluation.getBoard().getId(), board.getId()));
        List<ReviewBoardMemberEntity> activeMembers = reviewBoardMemberRepository
                .findByBoard_IdAndActiveTrueOrderByMemberRoleAscAssignedAtAsc(board.getId());
        List<EvaluationEntity> required = new ArrayList<>();
        for (ReviewBoardMemberEntity member : activeMembers) {
            List<EvaluationEntity> matches = evaluations.stream()
                    .filter(evaluation -> evaluation.getBoardMember() != null
                            && Objects.equals(evaluation.getBoardMember().getId(), member.getId())
                            && evaluation.getLecturer() != null
                            && Objects.equals(evaluation.getLecturer().getId(), member.getLecturer().getId()))
                    .toList();
            if (matches.size() != 1) {
                structurallyValid = false;
            } else {
                required.add(matches.get(0));
            }
        }
        return new EvaluationContext(required, board, structurallyValid && !activeMembers.isEmpty());
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

    private record EvaluationContext(
            List<EvaluationEntity> requiredEvaluations,
            ReviewBoardEntity board,
            boolean structurallyValid) {
    }

    public record StudentResultSummary(
            Long registrationId,
            Long groupId,
            String groupName,
            Long topicId,
            String topicTitle,
            Long departmentId,
            String departmentCode,
            String departmentName,
            Long periodId,
            String periodName,
            BigDecimal averageScore,
            String finalComment,
            LocalDateTime publishedAt) {

    }

    public record FilterOption(Long id, String label, String secondaryLabel) {
    }

    public record StudentResultPage(
            List<StudentResultSummary> results,
            int page,
            int size,
            int totalItems,
            int totalPages,
            String search,
            String sort,
            String direction,
            Long departmentId,
            Long periodId,
            List<FilterOption> departments,
            List<FilterOption> periods) {

        public StudentResultPage {
            results = List.copyOf(results);
            departments = List.copyOf(departments);
            periods = List.copyOf(periods);
        }

        public boolean hasPrevious() { return page > 0; }
        public boolean hasNext() { return page + 1 < totalPages; }
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
