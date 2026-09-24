package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
public class TopicReviewService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicReviewService(TopicRepository topicRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public List<TopicReviewSummary> listPending(String reviewerEmail) {
        return pendingTopics(reviewerEmail).stream().map(TopicReviewService::toSummary).toList();
    }

    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicReviewPage listPendingPage(
            String reviewerEmail, String search, int page, int size, String sort, String direction) {
        return listPendingPage(reviewerEmail, search, null, null, page, size, sort, direction);
    }

    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicReviewPage listPendingPage(
            String reviewerEmail, String search, Long departmentId, Long periodId,
            int page, int size, String sort, String direction) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        // pendingTopics() already applies the Admin / Faculty Head department scope, so the filter
        // options and filtered rows can never reach topics outside the reviewer's authority.
        List<TopicReviewSummary> scopedTopics = pendingTopics(reviewerEmail).stream()
                .map(TopicReviewService::toSummary)
                .toList();
        List<DepartmentFilterOption> departmentOptions = scopedTopics.stream()
                .map(topic -> new DepartmentFilterOption(
                        topic.getDepartmentId(), topic.getDepartmentCode(), topic.getDepartmentName()))
                .distinct()
                .sorted(Comparator.comparing(
                        DepartmentFilterOption::code, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<PeriodFilterOption> periodOptions = scopedTopics.stream()
                .map(topic -> new PeriodFilterOption(topic.getPeriodId(), topic.getPeriodName(), topic.getPeriodType()))
                .distinct()
                .sorted(Comparator.comparing(
                        PeriodFilterOption::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<TopicReviewSummary> filtered = scopedTopics.stream()
                .filter(topic -> departmentId == null || departmentId.equals(topic.getDepartmentId()))
                .filter(topic -> periodId == null || periodId.equals(topic.getPeriodId()))
                .filter(topic -> matchesSearch(topic, normalizedSearch))
                .sorted(topicComparator(normalizedSort, normalizedDirection))
                .toList();
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new TopicReviewPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection,
                departmentId, periodId, departmentOptions, periodOptions);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicReviewSummary review(String reviewerEmail, Long topicId, String decision) {
        UserEntity reviewer = findActiveReviewer(reviewerEmail);
        ReviewDecision reviewDecision = ReviewDecision.parse(decision);
        TopicEntity topic = topicRepository.findByIdForReview(topicId)
                .orElseThrow(() -> new TopicReviewNotFoundException(topicId));
        assertCanReview(reviewer, topic);
        if (topic.getStatus() != TopicStatus.PENDING_APPROVAL) {
            throw new TopicReviewValidationException(
                    "Only pending approval topic proposals can be approved or rejected.");
        }
        if (reviewer.getId().equals(topic.getProposedBy().getId())) {
            throw new TopicReviewAccessException("A lecturer cannot approve or reject their own topic proposal.");
        }
        topic.setStatus(reviewDecision.targetStatus());
        return toSummary(topicRepository.saveAndFlush(topic));
    }

    private List<TopicEntity> topicsForFacultyHead(UserEntity reviewer) {
        DepartmentEntity department = reviewer.getDepartment();
        if (department == null || department.getId() == null) {
            return List.of();
        }
        return topicRepository.findByDepartmentIdAndStatusForReview(
                department.getId(), TopicStatus.PENDING_APPROVAL);
    }

    private List<TopicEntity> pendingTopics(String reviewerEmail) {
        UserEntity reviewer = findActiveReviewer(reviewerEmail);
        return isAdmin(reviewer)
                ? topicRepository.findByStatusForReview(TopicStatus.PENDING_APPROVAL)
                : topicsForFacultyHead(reviewer);
    }

    private static boolean matchesSearch(TopicReviewSummary topic, String search) {
        return search.isBlank()
                || containsIgnoreCase(topic.getTitle(), search)
                || containsIgnoreCase(topic.getDescription(), search)
                || containsIgnoreCase(topic.getDepartmentCode(), search)
                || containsIgnoreCase(topic.getDepartmentName(), search)
                || containsIgnoreCase(topic.getPeriodName(), search)
                || containsIgnoreCase(topic.getPeriodType(), search)
                || containsIgnoreCase(topic.getProposedByName(), search)
                || containsIgnoreCase(topic.getProposedByEmail(), search)
                || containsIgnoreCase(topic.getStatusLabel(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<TopicReviewSummary> topicComparator(String sort, String direction) {
        Comparator<TopicReviewSummary> comparator = switch (sort) {
            case "proposer" -> Comparator.comparing(
                    TopicReviewSummary::getProposedByName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "department" -> Comparator.comparing(
                    TopicReviewSummary::getDepartmentCode,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    TopicReviewSummary::getPeriodName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(
                    TopicReviewSummary::getStatusLabel,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "updated" -> Comparator.comparing(
                    TopicReviewSummary::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    TopicReviewSummary::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicReviewSummary::getProposedByName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        if ("desc".equals(direction)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "proposer", "department", "period", "status", "updated" ->
                    sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private UserEntity findActiveReviewer(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> isAdmin(user) || hasActiveRole(user, "FACULTY_HEAD"))
                .orElseThrow(() -> new TopicReviewAccessException(
                        "Only an active Admin or Faculty Head can review topic proposals."));
    }

    private void assertCanReview(UserEntity reviewer, TopicEntity topic) {
        if (isAdmin(reviewer)) {
            return;
        }
        DepartmentEntity department = reviewer.getDepartment();
        if (department == null || topic.getDepartment() == null
                || !department.getId().equals(topic.getDepartment().getId())) {
            throw new TopicReviewAccessException(
                    "Faculty Heads can only review topic proposals in their department.");
        }
    }

    private static boolean isAdmin(UserEntity user) {
        return hasActiveRole(user, "ADMIN");
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(RoleEntity::isActive)
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static TopicReviewSummary toSummary(TopicEntity topic) {
        return new TopicReviewSummary(
                topic.getId(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getDepartment().getId(),
                topic.getDepartment().getCode(),
                topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getId(),
                topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().name(),
                topic.getProposedBy().getId(),
                topic.getProposedBy().getFullName(),
                topic.getProposedBy().getEmailOrCode(),
                topic.getStatus().name(),
                statusLabel(topic.getStatus()),
                topic.getUpdatedAt());
    }

    private static String statusLabel(TopicStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case PENDING_APPROVAL -> "Pending approval";
            case REJECTED -> "Rejected";
            case APPROVED -> "Approved";
            case PUBLISHED -> "Published";
        };
    }

    public enum ReviewDecision {
        APPROVE(TopicStatus.APPROVED),
        REJECT(TopicStatus.REJECTED);

        private final TopicStatus targetStatus;

        ReviewDecision(TopicStatus targetStatus) {
            this.targetStatus = targetStatus;
        }

        public TopicStatus targetStatus() {
            return targetStatus;
        }

        static ReviewDecision parse(String value) {
            if (value == null) {
                throw new TopicReviewValidationException("Review decision must be approve or reject.");
            }
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new TopicReviewValidationException("Review decision must be approve or reject.");
            }
        }
    }

    public static final class TopicReviewSummary {
        private final Long id;
        private final String title;
        private final String description;
        private final Long departmentId;
        private final String departmentCode;
        private final String departmentName;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final Long proposedById;
        private final String proposedByName;
        private final String proposedByEmail;
        private final String statusCode;
        private final String statusLabel;
        private final LocalDateTime updatedAt;

        public TopicReviewSummary(
                Long id, String title, String description, Long departmentId, String departmentCode,
                String departmentName, Long periodId, String periodName, String periodType, Long proposedById,
                String proposedByName, String proposedByEmail, String statusCode, String statusLabel,
                LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.proposedById = proposedById;
            this.proposedByName = proposedByName;
            this.proposedByEmail = proposedByEmail;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Long getDepartmentId() { return departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public Long getProposedById() { return proposedById; }
        public String getProposedByName() { return proposedByName; }
        public String getProposedByEmail() { return proposedByEmail; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static final class TopicReviewPage {
        private final List<TopicReviewSummary> topics;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final Long departmentId;
        private final Long periodId;
        private final List<DepartmentFilterOption> departmentOptions;
        private final List<PeriodFilterOption> periodOptions;

        public TopicReviewPage(
                List<TopicReviewSummary> topics, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction) {
            this(topics, page, size, totalItems, totalPages, search, sort, direction,
                    null, null, List.of(), List.of());
        }

        public TopicReviewPage(
                List<TopicReviewSummary> topics, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction, Long departmentId, Long periodId,
                List<DepartmentFilterOption> departmentOptions, List<PeriodFilterOption> periodOptions) {
            this.topics = List.copyOf(topics);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
            this.departmentId = departmentId;
            this.periodId = periodId;
            this.departmentOptions = List.copyOf(departmentOptions);
            this.periodOptions = List.copyOf(periodOptions);
        }

        public List<TopicReviewSummary> getTopics() { return topics; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public Long getDepartmentId() { return departmentId; }
        public Long getPeriodId() { return periodId; }
        public List<DepartmentFilterOption> getDepartmentOptions() { return departmentOptions; }
        public List<PeriodFilterOption> getPeriodOptions() { return periodOptions; }
        public boolean isFiltered() {
            return departmentId != null || periodId != null || (search != null && !search.isBlank());
        }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public record DepartmentFilterOption(Long id, String code, String name) {
    }

    public record PeriodFilterOption(Long id, String name, String type) {
    }

    public static class TopicReviewNotFoundException extends RuntimeException {
        public TopicReviewNotFoundException(Long topicId) {
            super(topicId == null ? "Topic id is required." : "Topic proposal not found: " + topicId);
        }
    }

    public static class TopicReviewValidationException extends RuntimeException {
        public TopicReviewValidationException(String message) {
            super(message);
        }
    }

    public static class TopicReviewAccessException extends RuntimeException {
        public TopicReviewAccessException(String message) {
            super(message);
        }
    }
}
