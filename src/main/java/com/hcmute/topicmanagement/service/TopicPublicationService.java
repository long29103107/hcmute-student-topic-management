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
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicPublicationService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;

    public TopicPublicationService(
            TopicRepository topicRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RegistrationPeriodRepository registrationPeriodRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
    }

    /** Lists approved topics in the authenticated publisher's allowed scope. */
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicPublicationPage listApprovedPage(
            String facultyHeadEmail, String search, int page, int size, String sort, String direction) {
        UserEntity publisher = findActivePublisher(facultyHeadEmail);
        List<TopicPublicationSummary> filtered = approvedTopics(publisher).stream()
                .map(TopicPublicationService::toSummary)
                .filter(topic -> matchesSearch(topic, normalizeSearch(search)))
                .sorted(topicComparator(normalizeSort(sort), normalizeDirection(direction)))
                .toList();
        return paginate(filtered, page, size, search, sort, direction, null, null);
    }

    /** Moves one approved topic to the immutable published state. */
    @Transactional
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicPublicationSummary publish(String publisherEmail, Long topicId) {
        UserEntity publisher = findActivePublisher(publisherEmail);
        TopicEntity topic = topicRepository.findByIdForReview(topicId)
                .orElseThrow(() -> new TopicPublicationNotFoundException(topicId));
        assertCanPublish(publisher, topic);
        if (topic.getStatus() != TopicStatus.APPROVED) {
            throw new TopicPublicationValidationException(
                    "Only approved topic proposals can be published.");
        }
        topic.setStatus(TopicStatus.PUBLISHED);
        return toSummary(topicRepository.saveAndFlush(topic));
    }

    /** Returns only published topics in an open period and active student window. */
    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public List<PublishedTopicSummary> listPublished(
            String search, Long departmentId, Long periodId, LocalDateTime now) {
        String normalizedSearch = normalizeSearch(search);
        return publishedTopics(now).stream()
                .map(TopicPublicationService::toPublishedSummary)
                .filter(topic -> departmentId == null || departmentId.equals(topic.getDepartmentId()))
                .filter(topic -> periodId == null || periodId.equals(topic.getPeriodId()))
                .filter(topic -> matchesSearch(topic, normalizedSearch))
                .toList();
    }

    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public PublishedTopicPage listPublishedPage(
            String search, Long departmentId, Long periodId,
            int page, int size, String sort, String direction, LocalDateTime now) {
        List<PublishedTopicSummary> filtered = listPublished(search, departmentId, periodId, now).stream()
                .sorted(publishedComparator(normalizeSort(sort), normalizeDirection(direction)))
                .toList();
        return paginatePublished(filtered, page, size, search, sort, direction, departmentId, periodId);
    }

    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public List<FilterOption> listDepartmentOptions() {
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(department -> new FilterOption(
                        department.getId(), department.getCode(), department.getName()))
                .toList();
    }

    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public List<FilterOption> listPeriodOptions(LocalDateTime now) {
        return registrationPeriodRepository.findOpenForStudent(
                        RegistrationPeriodStatus.OPEN, normalizeNow(now)).stream()
                .map(period -> new FilterOption(period.getId(), period.getName(), period.getType().name()))
                .toList();
    }

    private List<TopicEntity> approvedTopics(UserEntity publisher) {
        if (isAdmin(publisher)) {
            return topicRepository.findByStatusForPublication(TopicStatus.APPROVED);
        }
        DepartmentEntity department = publisher.getDepartment();
        if (department == null || department.getId() == null) {
            return List.of();
        }
        return topicRepository.findByDepartmentIdAndStatusForPublication(
                department.getId(), TopicStatus.APPROVED);
    }

    private List<TopicEntity> publishedTopics(LocalDateTime now) {
        return topicRepository.findPublishedForStudent(
                TopicStatus.PUBLISHED, RegistrationPeriodStatus.OPEN, normalizeNow(now));
    }

    private static void assertCanPublish(UserEntity publisher, TopicEntity topic) {
        if (isAdmin(publisher)) {
            return;
        }
        DepartmentEntity department = publisher.getDepartment();
        if (department == null || topic.getDepartment() == null
                || !department.getId().equals(topic.getDepartment().getId())) {
            throw new TopicPublicationAccessException(
                    "Faculty Heads can only publish topics in their department.");
        }
    }

    private UserEntity findActivePublisher(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> isAdmin(user) || isFacultyHead(user))
                .orElseThrow(() -> new TopicPublicationAccessException(
                        "Only an active Admin or Faculty Head can publish topics."));
    }

    private static boolean isAdmin(UserEntity user) {
        return hasActiveRole(user, "ADMIN");
    }

    private static boolean isFacultyHead(UserEntity user) {
        return hasActiveRole(user, "FACULTY_HEAD");
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(RoleEntity::isActive)
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean matchesSearch(TopicPublicationSummary topic, String search) {
        return search.isBlank()
                || containsIgnoreCase(topic.getTitle(), search)
                || containsIgnoreCase(topic.getDescription(), search)
                || containsIgnoreCase(topic.getDepartmentCode(), search)
                || containsIgnoreCase(topic.getDepartmentName(), search)
                || containsIgnoreCase(topic.getPeriodName(), search)
                || containsIgnoreCase(topic.getPeriodType(), search)
                || containsIgnoreCase(topic.getProposedByName(), search)
                || containsIgnoreCase(topic.getProposedByEmail(), search);
    }

    private static boolean matchesSearch(PublishedTopicSummary topic, String search) {
        return search.isBlank()
                || containsIgnoreCase(topic.getTitle(), search)
                || containsIgnoreCase(topic.getDescription(), search)
                || containsIgnoreCase(topic.getDepartmentCode(), search)
                || containsIgnoreCase(topic.getDepartmentName(), search)
                || containsIgnoreCase(topic.getPeriodName(), search)
                || containsIgnoreCase(topic.getPeriodType(), search)
                || containsIgnoreCase(topic.getProposedByName(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<TopicPublicationSummary> topicComparator(String sort, String direction) {
        Comparator<TopicPublicationSummary> comparator = switch (sort) {
            case "department" -> Comparator.comparing(
                    TopicPublicationSummary::getDepartmentCode,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicPublicationSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    TopicPublicationSummary::getPeriodName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicPublicationSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "proposer" -> Comparator.comparing(
                    TopicPublicationSummary::getProposedByName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(TopicPublicationSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "updated" -> Comparator.comparing(
                    TopicPublicationSummary::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TopicPublicationSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    TopicPublicationSummary::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicPublicationSummary::getPeriodName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static Comparator<PublishedTopicSummary> publishedComparator(String sort, String direction) {
        Comparator<PublishedTopicSummary> comparator = switch (sort) {
            case "department" -> Comparator.comparing(
                    PublishedTopicSummary::getDepartmentCode,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(PublishedTopicSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    PublishedTopicSummary::getPeriodName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(PublishedTopicSummary::getTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    PublishedTopicSummary::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PublishedTopicSummary::getDepartmentCode,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "department", "period", "proposer", "updated" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    private static LocalDateTime normalizeNow(LocalDateTime now) {
        return now == null ? LocalDateTime.now() : now;
    }

    private static TopicPublicationPage paginate(
            List<TopicPublicationSummary> topics, int page, int size,
            String search, String sort, String direction, Long departmentId, Long periodId) {
        int safeSize = safeSize(size);
        int totalItems = topics.size();
        int totalPages = totalPages(totalItems, safeSize);
        int safePage = safePage(page, totalPages);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new TopicPublicationPage(
                topics.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizeSearch(search), normalizeSort(sort), normalizeDirection(direction), departmentId, periodId);
    }

    private static PublishedTopicPage paginatePublished(
            List<PublishedTopicSummary> topics, int page, int size,
            String search, String sort, String direction, Long departmentId, Long periodId) {
        int safeSize = safeSize(size);
        int totalItems = topics.size();
        int totalPages = totalPages(totalItems, safeSize);
        int safePage = safePage(page, totalPages);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new PublishedTopicPage(
                topics.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizeSearch(search), normalizeSort(sort), normalizeDirection(direction), departmentId, periodId);
    }

    private static int safeSize(int size) {
        return Math.min(Math.max(size, 5), 100);
    }

    private static int totalPages(int totalItems, int size) {
        return Math.max(1, (int) Math.ceil((double) totalItems / size));
    }

    private static int safePage(int page, int totalPages) {
        return Math.min(Math.max(page, 0), totalPages - 1);
    }

    private static TopicPublicationSummary toSummary(TopicEntity topic) {
        return new TopicPublicationSummary(
                topic.getId(), topic.getTitle(), topic.getDescription(),
                topic.getDepartment().getId(), topic.getDepartment().getCode(), topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getId(), topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().name(), topic.getProposedBy().getId(),
                topic.getProposedBy().getFullName(), topic.getProposedBy().getEmailOrCode(),
                topic.getStatus().name(), statusLabel(topic.getStatus()), topic.getUpdatedAt());
    }

    private static PublishedTopicSummary toPublishedSummary(TopicEntity topic) {
        return new PublishedTopicSummary(
                topic.getId(), topic.getTitle(), topic.getDescription(),
                topic.getDepartment().getId(), topic.getDepartment().getCode(), topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getId(), topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().name(), topic.getProposedBy().getFullName(),
                topic.getStatus().name(), statusLabel(topic.getStatus()), topic.getUpdatedAt());
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

    public static final class FilterOption {
        private final Long id;
        private final String label;
        private final String secondaryLabel;

        public FilterOption(Long id, String label, String secondaryLabel) {
            this.id = id;
            this.label = label;
            this.secondaryLabel = secondaryLabel;
        }

        public Long getId() { return id; }
        public String getLabel() { return label; }
        public String getSecondaryLabel() { return secondaryLabel; }
    }

    public static class TopicPublicationSummary {
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

        public TopicPublicationSummary(
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

    public static final class PublishedTopicSummary {
        private final Long id;
        private final String title;
        private final String description;
        private final Long departmentId;
        private final String departmentCode;
        private final String departmentName;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final String proposedByName;
        private final String statusCode;
        private final String statusLabel;
        private final LocalDateTime updatedAt;

        public PublishedTopicSummary(
                Long id, String title, String description, Long departmentId, String departmentCode,
                String departmentName, Long periodId, String periodName, String periodType,
                String proposedByName, String statusCode, String statusLabel, LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.proposedByName = proposedByName;
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
        public String getProposedByName() { return proposedByName; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static final class TopicPublicationPage {
        private final List<TopicPublicationSummary> topics;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final Long departmentId;
        private final Long periodId;

        public TopicPublicationPage(
                List<TopicPublicationSummary> topics, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction, Long departmentId, Long periodId) {
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
        }

        public List<TopicPublicationSummary> getTopics() { return topics; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public Long getDepartmentId() { return departmentId; }
        public Long getPeriodId() { return periodId; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class PublishedTopicPage {
        private final List<PublishedTopicSummary> topics;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final Long departmentId;
        private final Long periodId;

        public PublishedTopicPage(
                List<PublishedTopicSummary> topics, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction, Long departmentId, Long periodId) {
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
        }

        public List<PublishedTopicSummary> getTopics() { return topics; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public Long getDepartmentId() { return departmentId; }
        public Long getPeriodId() { return periodId; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static class TopicPublicationNotFoundException extends RuntimeException {
        public TopicPublicationNotFoundException(Long topicId) {
            super(topicId == null ? "Topic id is required." : "Topic proposal not found: " + topicId);
        }
    }

    public static class TopicPublicationValidationException extends RuntimeException {
        public TopicPublicationValidationException(String message) {
            super(message);
        }
    }

    public static class TopicPublicationAccessException extends RuntimeException {
        public TopicPublicationAccessException(String message) {
            super(message);
        }
    }
}
