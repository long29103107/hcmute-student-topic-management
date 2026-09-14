package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicRegistrationReviewService {

    private final TopicRegistrationRepository topicRegistrationRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public TopicRegistrationReviewService(
            TopicRegistrationRepository topicRegistrationRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public List<RegistrationReviewSummary> listPending(String reviewerEmail) {
        return pendingRegistrations(reviewerEmail).stream()
                .map(TopicRegistrationReviewService::toSummary)
                .toList();
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public RegistrationReviewPage listPendingPage(
            String reviewerEmail, String search, Long departmentId,
            int page, int size, String sort, String direction) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        List<RegistrationReviewSummary> filtered = pendingRegistrations(reviewerEmail).stream()
                .map(TopicRegistrationReviewService::toSummary)
            .filter(registration -> departmentId == null || departmentId.equals(registration.getDepartmentId()))
                .filter(registration -> matchesSearch(registration, normalizedSearch))
                .sorted(registrationComparator(normalizedSort, normalizedDirection))
                .toList();
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new RegistrationReviewPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection, departmentId);
    }

    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public List<DepartmentOption> listDepartmentOptions(String reviewerEmail) {
        UserEntity reviewer = findActiveReviewer(reviewerEmail);
        if (isAdmin(reviewer)) {
            return departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                    .map(TopicRegistrationReviewService::toDepartmentOption)
                    .toList();
        }
        DepartmentEntity department = reviewer.getDepartment();
        return department == null || !department.isActive()
                ? List.of()
                : List.of(toDepartmentOption(department));
    }

    @Transactional
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public RegistrationReviewSummary review(
            String reviewerEmail, Long registrationId, String decision, String rejectionReason) {
        UserEntity reviewer = findActiveReviewer(reviewerEmail);
        ReviewDecision reviewDecision = ReviewDecision.parse(decision);
        TopicRegistrationEntity registration = topicRegistrationRepository.findByIdForReview(registrationId)
                .orElseThrow(() -> new TopicRegistrationReviewNotFoundException(registrationId));
        assertCanReview(reviewer, registration);
        if (registration.getStatus() != TopicRegistrationStatus.PENDING) {
            throw new TopicRegistrationReviewValidationException(
                    "Only pending topic registrations can be approved or rejected.");
        }

        if (reviewDecision == ReviewDecision.REJECT) {
            String normalizedReason = normalizeReason(rejectionReason);
            if (normalizedReason.isBlank()) {
                throw new TopicRegistrationReviewValidationException(
                        "A rejection reason is required when rejecting a topic registration.");
            }
            if (normalizedReason.length() > 500) {
                throw new TopicRegistrationReviewValidationException(
                        "Rejection reason must be at most 500 characters.");
            }
            registration.setRejectionReason(normalizedReason);
        } else {
            registration.setRejectionReason(null);
        }
        registration.setStatus(reviewDecision.targetStatus());
        return toSummary(topicRegistrationRepository.saveAndFlush(registration));
    }

    private List<TopicRegistrationEntity> pendingRegistrations(String reviewerEmail) {
        UserEntity reviewer = findActiveReviewer(reviewerEmail);
        return topicRegistrationRepository.findByStatusForReview(TopicRegistrationStatus.PENDING).stream()
                .filter(registration -> isAdmin(reviewer) || belongsToDepartment(reviewer, registration))
                .toList();
    }

    private static boolean belongsToDepartment(UserEntity reviewer, TopicRegistrationEntity registration) {
        DepartmentEntity reviewerDepartment = reviewer.getDepartment();
        DepartmentEntity topicDepartment = registration.getTopic().getDepartment();
        return reviewerDepartment != null
                && topicDepartment != null
                && reviewerDepartment.getId() != null
                && reviewerDepartment.getId().equals(topicDepartment.getId());
    }

    private void assertCanReview(UserEntity reviewer, TopicRegistrationEntity registration) {
        if (isAdmin(reviewer)) {
            return;
        }
        if (!belongsToDepartment(reviewer, registration)) {
            throw new TopicRegistrationReviewAccessException(
                    "Faculty Heads can only review registrations for their department.");
        }
    }

    private UserEntity findActiveReviewer(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> isAdmin(user) || hasActiveRole(user, "FACULTY_HEAD"))
                .orElseThrow(() -> new TopicRegistrationReviewAccessException(
                        "Only an active Admin or Faculty Head can review topic registrations."));
    }

    private static boolean isAdmin(UserEntity user) {
        return hasActiveRole(user, "ADMIN");
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.isActive())
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean matchesSearch(RegistrationReviewSummary registration, String search) {
        return search.isBlank()
                || containsIgnoreCase(registration.getGroupName(), search)
                || containsIgnoreCase(registration.getGroupLeaderName(), search)
                || containsIgnoreCase(registration.getGroupLeaderLogin(), search)
                || containsIgnoreCase(registration.getTopicTitle(), search)
                || containsIgnoreCase(registration.getDepartmentCode(), search)
                || containsIgnoreCase(registration.getDepartmentName(), search)
                || containsIgnoreCase(registration.getPeriodName(), search)
                || containsIgnoreCase(registration.getSubmittedByName(), search)
                || containsIgnoreCase(registration.getSubmittedByLogin(), search)
                || containsIgnoreCase(registration.getStatusLabel(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<RegistrationReviewSummary> registrationComparator(String sort, String direction) {
        Comparator<RegistrationReviewSummary> comparator = switch (sort) {
            case "group" -> Comparator.comparing(
                    RegistrationReviewSummary::getGroupName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "department" -> Comparator.comparing(
                    RegistrationReviewSummary::getDepartmentCode,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    RegistrationReviewSummary::getPeriodName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "submitted" -> Comparator.comparing(
                    RegistrationReviewSummary::getSubmittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(
                    RegistrationReviewSummary::getStatusLabel,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    RegistrationReviewSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(RegistrationReviewSummary::getGroupName, String.CASE_INSENSITIVE_ORDER);
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "group", "department", "period", "submitted", "status" ->
                    sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private static String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim();
    }

    private static DepartmentOption toDepartmentOption(DepartmentEntity department) {
        return new DepartmentOption(department.getId(), department.getCode(), department.getName());
    }

    private static RegistrationReviewSummary toSummary(TopicRegistrationEntity registration) {
        return new RegistrationReviewSummary(
                registration.getId(),
                registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(),
                registration.getStudentGroup().getLeader().getFullName(),
                registration.getStudentGroup().getLeader().getEmailOrCode(),
                registration.getTopic().getId(),
                registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getId(),
                registration.getTopic().getDepartment().getCode(),
                registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(),
                registration.getRegistrationPeriod().getType().name(),
                registration.getSubmittedBy().getFullName(),
                registration.getSubmittedBy().getEmailOrCode(),
                registration.getSubmittedAt(),
                registration.getStatus().name(),
                statusLabel(registration.getStatus()),
                registration.getRejectionReason());
    }

    private static String statusLabel(TopicRegistrationStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    public enum ReviewDecision {
        APPROVE(TopicRegistrationStatus.APPROVED),
        REJECT(TopicRegistrationStatus.REJECTED);

        private final TopicRegistrationStatus targetStatus;

        ReviewDecision(TopicRegistrationStatus targetStatus) {
            this.targetStatus = targetStatus;
        }

        public TopicRegistrationStatus targetStatus() {
            return targetStatus;
        }

        static ReviewDecision parse(String value) {
            if (value == null) {
                throw new TopicRegistrationReviewValidationException(
                        "Review decision must be approve or reject.");
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new TopicRegistrationReviewValidationException(
                        "Review decision must be approve or reject.");
            }
        }
    }

    public static final class RegistrationReviewSummary {
        private final Long id;
        private final Long groupId;
        private final String groupName;
        private final String groupLeaderName;
        private final String groupLeaderLogin;
        private final Long topicId;
        private final String topicTitle;
        private final Long departmentId;
        private final String departmentCode;
        private final String departmentName;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final String submittedByName;
        private final String submittedByLogin;
        private final LocalDateTime submittedAt;
        private final String statusCode;
        private final String statusLabel;
        private final String rejectionReason;

        public RegistrationReviewSummary(
                Long id, Long groupId, String groupName, String groupLeaderName, String groupLeaderLogin,
                Long topicId, String topicTitle, Long departmentId, String departmentCode,
                String departmentName, Long periodId, String periodName, String periodType,
                String submittedByName, String submittedByLogin, LocalDateTime submittedAt,
                String statusCode, String statusLabel, String rejectionReason) {
            this.id = id;
            this.groupId = groupId;
            this.groupName = groupName;
            this.groupLeaderName = groupLeaderName;
            this.groupLeaderLogin = groupLeaderLogin;
            this.topicId = topicId;
            this.topicTitle = topicTitle;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.submittedByName = submittedByName;
            this.submittedByLogin = submittedByLogin;
            this.submittedAt = submittedAt;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.rejectionReason = rejectionReason;
        }

        public Long getId() { return id; }
        public Long getGroupId() { return groupId; }
        public String getGroupName() { return groupName; }
        public String getGroupLeaderName() { return groupLeaderName; }
        public String getGroupLeaderLogin() { return groupLeaderLogin; }
        public Long getTopicId() { return topicId; }
        public String getTopicTitle() { return topicTitle; }
        public Long getDepartmentId() { return departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public String getSubmittedByName() { return submittedByName; }
        public String getSubmittedByLogin() { return submittedByLogin; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public String getRejectionReason() { return rejectionReason; }
    }

    public static final class RegistrationReviewPage {
        private final List<RegistrationReviewSummary> registrations;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final Long departmentId;

        public RegistrationReviewPage(
                List<RegistrationReviewSummary> registrations, int page, int size, int totalItems,
            int totalPages, String search, String sort, String direction, Long departmentId) {
            this.registrations = List.copyOf(registrations);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
            this.departmentId = departmentId;
        }

        public List<RegistrationReviewSummary> getRegistrations() { return registrations; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public Long getDepartmentId() { return departmentId; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class DepartmentOption {
        private final Long id;
        private final String code;
        private final String name;

        public DepartmentOption(Long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
    }

    public static class TopicRegistrationReviewNotFoundException extends RuntimeException {
        public TopicRegistrationReviewNotFoundException(Long registrationId) {
            super(registrationId == null
                    ? "Registration id is required."
                    : "Topic registration not found: " + registrationId);
        }
    }

    public static class TopicRegistrationReviewValidationException extends RuntimeException {
        public TopicRegistrationReviewValidationException(String message) {
            super(message);
        }
    }

    public static class TopicRegistrationReviewAccessException extends RuntimeException {
        public TopicRegistrationReviewAccessException(String message) {
            super(message);
        }
    }
}
