package com.hcmute.topicmanagement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.config.EvaluationScoringProperties;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class EvaluationScoringService {

    private static final int COMMENT_MAX_LENGTH = 10_000;

    private final EvaluationRepository evaluationRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final UserRepository userRepository;
    private final EvaluationScoringProperties scoringProperties;
    private final EvaluationScoreCalculator scoreCalculator;

    public EvaluationScoringService(
            EvaluationRepository evaluationRepository,
            RegistrationResultRepository registrationResultRepository,
            UserRepository userRepository,
            EvaluationScoringProperties scoringProperties,
            EvaluationScoreCalculator scoreCalculator) {
        this.evaluationRepository = evaluationRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.userRepository = userRepository;
        this.scoringProperties = scoringProperties;
        this.scoreCalculator = scoreCalculator;
    }

    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public ScoringPage listAssigned(String evaluatorEmail) {
        return new ScoringPage(
                loadAssigned(evaluatorEmail), scoringProperties.getMinimumScore(), scoringProperties.getMaximumScore());
    }

    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public ScoringPage listAssignedPage(
            String evaluatorEmail, String search, int page, int size, String sort, String direction) {
        List<EvaluationSummary> summaries = loadAssigned(evaluatorEmail);
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        List<EvaluationSummary> filtered = summaries.stream()
                .filter(evaluation -> matchesSearch(evaluation, normalizedSearch))
                .sorted(evaluationComparator(normalizedSort, normalizedDirection))
                .toList();
        int safeSize = Math.min(Math.max(size, 5), 100);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new ScoringPage(
                filtered.subList(from, to), scoringProperties.getMinimumScore(), scoringProperties.getMaximumScore(),
                safePage, safeSize, totalItems, totalPages, normalizedSearch, normalizedSort, normalizedDirection);
    }

    private List<EvaluationSummary> loadAssigned(String evaluatorEmail) {
        UserEntity evaluator = findActiveEvaluator(evaluatorEmail);
        return evaluationRepository
                .findByLecturer_IdOrderByUpdatedAtDesc(evaluator.getId()).stream()
                .filter(evaluation -> evaluation.getTopicRegistration() != null
                        && evaluation.getTopicRegistration().getStatus() == TopicRegistrationStatus.APPROVED)
                .filter(this::isVisibleAssignment)
                .map(this::toSummary)
                .toList();
    }

    private static boolean matchesSearch(EvaluationSummary evaluation, String search) {
        return search.isBlank()
                || contains(evaluation.groupName(), search)
                || contains(evaluation.topicTitle(), search)
                || contains(evaluation.departmentCode(), search)
                || contains(evaluation.departmentName(), search)
                || contains(evaluation.periodName(), search)
                || contains(evaluation.status(), search);
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<EvaluationSummary> evaluationComparator(String sort, String direction) {
        Comparator<EvaluationSummary> comparator = switch (sort) {
            case "topic" -> Comparator.comparing(
                    EvaluationSummary::topicTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "period" -> Comparator.comparing(
                    EvaluationSummary::periodName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "score" -> Comparator.comparing(
                    EvaluationSummary::score, Comparator.nullsLast(Comparator.naturalOrder()));
            case "average" -> Comparator.comparing(
                    EvaluationSummary::averageScore, Comparator.nullsLast(Comparator.naturalOrder()));
            case "status" -> Comparator.comparing(
                    EvaluationSummary::status, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "submitted" -> Comparator.comparing(
                    EvaluationSummary::submittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(
                    EvaluationSummary::groupName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EvaluationSummary::topicTitle,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "topic", "period", "score", "average", "status", "submitted" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "group";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    @Transactional
    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public EvaluationSummary submitScore(
            String evaluatorEmail, Long evaluationId, BigDecimal score, String comment) {
        if (evaluationId == null || evaluationId <= 0) {
            throw new EvaluationScoringValidationException("Evaluation id is invalid.");
        }
        UserEntity evaluator = findActiveEvaluator(evaluatorEmail);
        EvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationScoringNotFoundException(evaluationId));
        if (evaluation.getLecturer() == null
                || !Objects.equals(evaluator.getId(), evaluation.getLecturer().getId())) {
            throw new EvaluationScoringAccessException(
                    "Only the assigned evaluator can submit this evaluation.");
        }
        if (evaluation.getBoard() != null && (evaluation.getBoardMember() == null
                || !evaluation.getBoardMember().isActive()
                || (evaluation.getBoard().getStatus() != com.hcmute.topicmanagement.model.enums.ReviewBoardStatus.ACTIVE
                && evaluation.getBoard().getStatus() != com.hcmute.topicmanagement.model.enums.ReviewBoardStatus.COMPLETED))) {
            throw new EvaluationScoringValidationException(
                    "Board evaluations can only be scored by an active assigned member while the board is active.");
        }

        TopicRegistrationEntity registration = evaluation.getTopicRegistration();
        if (registration == null || registration.getStatus() != TopicRegistrationStatus.APPROVED) {
            throw new EvaluationScoringValidationException(
                    "Only evaluations for approved topic registrations can be submitted.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isResultPublished(registration)) {
            throw new EvaluationScoringValidationException(
                    "Evaluation cannot be changed after the result is published.");
        }
        LocalDateTime deadline = registration.getRegistrationPeriod().getReviewerScoreDeadline();
        if (deadline != null && now.isAfter(deadline)) {
            throw new EvaluationScoringValidationException(
                    "Evaluation cannot be changed after the reviewer score deadline.");
        }

        BigDecimal normalizedScore = validateScore(score);
        String normalizedComment = normalizeComment(comment);
        evaluation.setScore(normalizedScore);
        evaluation.setComment(normalizedComment);
        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(now);
        return toSummary(evaluationRepository.saveAndFlush(evaluation));
    }

    private UserEntity findActiveEvaluator(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(this::hasEvaluatorRole)
                .orElseThrow(() -> new EvaluationScoringAccessException(
                        "Only an active Lecturer or Faculty Head can submit evaluations."));
    }

    private boolean hasEvaluatorRole(UserEntity user) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive()
                        && ("LECTURER".equalsIgnoreCase(role.getCode())
                                || "FACULTY_HEAD".equalsIgnoreCase(role.getCode())
                                || "ADMIN".equalsIgnoreCase(role.getCode())));
    }

    private EvaluationSummary toSummary(EvaluationEntity evaluation) {
        TopicRegistrationEntity registration = evaluation.getTopicRegistration();
        LocalDateTime deadline = registration.getRegistrationPeriod().getReviewerScoreDeadline();
        boolean resultPublished = isResultPublished(registration);
        boolean deadlinePassed = deadline != null && LocalDateTime.now().isAfter(deadline);
        boolean boardEditable = evaluation.getBoard() == null || (evaluation.getBoardMember() != null
                && evaluation.getBoardMember().isActive()
                && (evaluation.getBoard().getStatus() == com.hcmute.topicmanagement.model.enums.ReviewBoardStatus.ACTIVE
                || evaluation.getBoard().getStatus() == com.hcmute.topicmanagement.model.enums.ReviewBoardStatus.COMPLETED));
        return new EvaluationSummary(
                evaluation.getId(),
                registration.getId(),
                registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                evaluation.getScore(),
                evaluation.getComment(),
                evaluation.getStatus().name(),
                averageScore(registration.getId()),
                evaluation.getSubmittedAt(),
                deadline,
                boardEditable && !resultPublished && !deadlinePassed,
                evaluation.getBoard() == null ? null : evaluation.getBoard().getStatus().name(),
                evaluation.getBoardMember() == null ? null : evaluation.getBoardMember().getMemberRole().name());
    }

    private boolean isVisibleAssignment(EvaluationEntity evaluation) {
        return evaluation.getBoard() == null
                || (evaluation.getBoardMember() != null && evaluation.getBoardMember().isActive());
    }

    private BigDecimal averageScore(Long registrationId) {
        return scoreCalculator.averageScore(
                evaluationRepository.findByTopicRegistration_IdOrderByCreatedAtAsc(registrationId));
    }

    private boolean isResultPublished(TopicRegistrationEntity registration) {
        return registrationResultRepository.findByTopicRegistration_Id(registration.getId())
                .map(result -> result.getStatus() == RegistrationResultStatus.PUBLISHED)
                .orElse(false);
    }

    private BigDecimal validateScore(BigDecimal score) {
        if (score == null) {
            throw new EvaluationScoringValidationException("Score is required.");
        }
        if (score.scale() > 2) {
            throw new EvaluationScoringValidationException("Score can have at most two decimal places.");
        }
        if (score.compareTo(scoringProperties.getMinimumScore()) < 0
                || score.compareTo(scoringProperties.getMaximumScore()) > 0) {
            throw new EvaluationScoringValidationException(
                    "Score must be between " + scoringProperties.getMinimumScore()
                            + " and " + scoringProperties.getMaximumScore() + ".");
        }
        return score;
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        if (normalized.length() > COMMENT_MAX_LENGTH) {
            throw new EvaluationScoringValidationException(
                    "Evaluation comment must be at most " + COMMENT_MAX_LENGTH + " characters.");
        }
        return normalized.isBlank() ? null : normalized;
    }

    public record ScoringPage(
            List<EvaluationSummary> evaluations, BigDecimal minimumScore, BigDecimal maximumScore,
            int page, int size, int totalItems, int totalPages, String search, String sort, String direction) {
        public ScoringPage(
                List<EvaluationSummary> evaluations, BigDecimal minimumScore, BigDecimal maximumScore) {
            this(evaluations, minimumScore, maximumScore, 0, Math.max(5, evaluations.size()), evaluations.size(), 1,
                    "", "submitted", "desc");
        }

        public ScoringPage {
            evaluations = List.copyOf(evaluations);
        }

        public boolean hasPrevious() { return page > 0; }
        public boolean hasNext() { return page + 1 < totalPages; }
    }

    public record EvaluationSummary(
            Long id,
            Long registrationId,
            Long groupId,
            String groupName,
            Long topicId,
            String topicTitle,
            String departmentCode,
            String departmentName,
            Long periodId,
            String periodName,
            BigDecimal score,
            String comment,
            String status,
            BigDecimal averageScore,
            LocalDateTime submittedAt,
            LocalDateTime deadline,
            boolean editable,
            String boardStatus,
            String boardMemberRole) {
    }

    public static class EvaluationScoringNotFoundException extends RuntimeException {
        public EvaluationScoringNotFoundException(Long evaluationId) {
            super(evaluationId == null ? "Evaluation id is required." : "Evaluation not found: " + evaluationId);
        }
    }

    public static class EvaluationScoringValidationException extends RuntimeException {
        public EvaluationScoringValidationException(String message) {
            super(message);
        }
    }

    public static class EvaluationScoringAccessException extends RuntimeException {
        public EvaluationScoringAccessException(String message) {
            super(message);
        }
    }
}
