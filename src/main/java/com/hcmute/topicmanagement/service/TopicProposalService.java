package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicProposalService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;
    private static final Set<TopicStatus> EDITABLE_STATUSES =
            EnumSet.of(TopicStatus.DRAFT, TopicStatus.REJECTED);
    private static final Set<TopicStatus> SUBMITTABLE_STATUSES =
            EnumSet.of(TopicStatus.DRAFT, TopicStatus.REJECTED);

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodService registrationPeriodService;

    public TopicProposalService(
            TopicRepository topicRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RegistrationPeriodService registrationPeriodService) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.registrationPeriodService = registrationPeriodService;
    }

    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public List<TopicSummary> listOwnProposals(String lecturerEmail) {
        UserEntity lecturer = findActiveLecturer(lecturerEmail);
        return topicRepository.findOwnProposalsWithPeriodAndDepartment(lecturer.getId()).stream()
                .map(TopicProposalService::toSummary)
                .toList();
    }

    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicProposalPage listOwnProposalsPage(
            String lecturerEmail, int page, int size, String search, String sort, String direction) {
        List<TopicSummary> topics = listOwnProposals(lecturerEmail);
        String normalizedSearch = normalizeSearch(search);
        if (!normalizedSearch.isBlank()) {
            topics = topics.stream()
                    .filter(topic -> matchesSearch(topic, normalizedSearch))
                    .toList();
        }
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        Comparator<TopicSummary> comparator = comparatorFor(normalizedSort)
                .thenComparing(TopicSummary::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("desc".equals(normalizedDirection)) {
            comparator = comparator.reversed();
        }
        topics = topics.stream().sorted(comparator).toList();
        int safeSize = Math.min(Math.max(size, 5), 100);
        int totalItems = topics.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new TopicProposalPage(
                topics.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "topic", "department", "period", "status", "updated" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "updated";
        };
    }

    private static String normalizeDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    private static Comparator<TopicSummary> comparatorFor(String sort) {
        return switch (sort) {
            case "topic" -> Comparator.comparing(
                    TopicSummary::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "department" -> Comparator.comparing(
                    TopicSummary::getDepartmentCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "period" -> Comparator.comparing(
                    TopicSummary::getPeriodName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> Comparator.comparing(
                    TopicSummary::getStatusLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(
                    TopicSummary::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesSearch(TopicSummary topic, String search) {
        return containsSearchValue(topic.getTitle(), search)
                || containsSearchValue(topic.getDescription(), search)
                || containsSearchValue(topic.getDepartmentCode(), search)
                || containsSearchValue(topic.getDepartmentName(), search)
                || containsSearchValue(topic.getPeriodName(), search)
                || containsSearchValue(topic.getStatusLabel(), search);
    }

    private static boolean containsSearchValue(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public ProposalFormOptions getProposalFormOptions() {
        LocalDateTime now = LocalDateTime.now();
        List<DepartmentOption> departments = departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(department -> new DepartmentOption(
                        department.getId(), department.getCode(), department.getName()))
                .toList();
        List<PeriodOption> periods = registrationPeriodService
                .listOpenForLecturer(now).stream()
                .map(period -> new PeriodOption(period.getId(), period.getName(), period.getType().name()))
                .toList();
        return new ProposalFormOptions(departments, periods);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicSummary create(
            String lecturerEmail, String title, String description, Long departmentId, Long periodId) {
        UserEntity lecturer = findActiveLecturer(lecturerEmail);
        TopicInput input = validateInput(title, description);
        DepartmentEntity department = findActiveDepartment(departmentId);
        RegistrationPeriodEntity period = findOpenLecturerPeriod(periodId);

        TopicEntity topic = new TopicEntity(period, department, lecturer, input.title(), input.description());
        topic.setStatus(TopicStatus.DRAFT);
        return toSummary(topicRepository.saveAndFlush(topic));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicSummary update(
            Long topicId, String lecturerEmail, String title, String description, Long departmentId, Long periodId) {
        UserEntity lecturer = findActiveLecturer(lecturerEmail);
        TopicEntity topic = topicRepository.findOwnProposalWithPeriodAndDepartment(topicId, lecturer.getId())
                .orElseThrow(() -> new TopicProposalNotFoundException(topicId));
        if (!EDITABLE_STATUSES.contains(topic.getStatus())) {
            throw new TopicProposalValidationException(
                    "Only draft or rejected topic proposals can be edited.");
        }

        TopicInput input = validateInput(title, description);
        DepartmentEntity department = findActiveDepartment(departmentId);
        RegistrationPeriodEntity period = findOpenLecturerPeriod(periodId);
        topic.setTitle(input.title());
        topic.setDescription(input.description());
        topic.setDepartment(department);
        topic.setRegistrationPeriod(period);
        if (topic.getStatus() == TopicStatus.REJECTED) {
            topic.setStatus(TopicStatus.DRAFT);
        }
        return toSummary(topicRepository.saveAndFlush(topic));
    }

    @Transactional
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicSummary submitForReview(Long topicId, String lecturerEmail) {
        UserEntity lecturer = findActiveLecturer(lecturerEmail);
        TopicEntity topic = topicRepository.findOwnProposalWithPeriodAndDepartment(topicId, lecturer.getId())
                .orElseThrow(() -> new TopicProposalNotFoundException(topicId));
        if (!SUBMITTABLE_STATUSES.contains(topic.getStatus())) {
            throw new TopicProposalValidationException(
                    "Only draft or rejected topic proposals can be submitted for review.");
        }

        try {
            registrationPeriodService.requireOpenForLecturer(
                    topic.getRegistrationPeriod().getId(), LocalDateTime.now());
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException
                | RegistrationPeriodService.RegistrationPeriodAccessException exception) {
            throw new TopicProposalValidationException(
                    "The topic proposal can only be submitted while its registration period is open for lecturer proposals.");
        }

        topic.setStatus(TopicStatus.PENDING_APPROVAL);
        return toSummary(topicRepository.saveAndFlush(topic));
    }

    private UserEntity findActiveLecturer(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new TopicProposalValidationException("Lecturer account is not available."));
    }

    private DepartmentEntity findActiveDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new TopicProposalValidationException("Select an active department.");
        }
        return departmentRepository.findById(departmentId)
                .filter(DepartmentEntity::isActive)
                .orElseThrow(() -> new TopicProposalValidationException(
                        "The selected department is not active or does not exist."));
    }

    private RegistrationPeriodEntity findOpenLecturerPeriod(Long periodId) {
        if (periodId == null) {
            throw new TopicProposalValidationException("Select a registration period.");
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            return registrationPeriodService.requireOpenForLecturer(periodId, now);
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException
                | RegistrationPeriodService.RegistrationPeriodAccessException exception) {
            throw new TopicProposalValidationException(
                    "The selected registration period is not open for lecturer proposals.");
        }
    }

    private static TopicInput validateInput(String title, String description) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedTitle.isBlank()) {
            throw new TopicProposalValidationException("Topic title is required.");
        }
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new TopicProposalValidationException("Topic title must be at most 255 characters.");
        }
        if (normalizedDescription.isBlank()) {
            throw new TopicProposalValidationException("Topic description is required.");
        }
        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new TopicProposalValidationException("Topic description must be at most 5000 characters.");
        }
        return new TopicInput(normalizedTitle, normalizedDescription);
    }

    private static TopicSummary toSummary(TopicEntity topic) {
        TopicStatus status = topic.getStatus();
        return new TopicSummary(
                topic.getId(),
                topic.getTitle(),
                topic.getDescription(),
                status.name(),
                statusLabel(status),
                EDITABLE_STATUSES.contains(status),
                topic.getDepartment().getId(),
                topic.getDepartment().getCode(),
                topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getId(),
                topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().name(),
                topic.getCreatedAt(),
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

    public static final class TopicSummary {
        private final Long id;
        private final String title;
        private final String description;
        private final String statusCode;
        private final String statusLabel;
        private final boolean editable;
        private final Long departmentId;
        private final String departmentCode;
        private final String departmentName;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        public TopicSummary(
                Long id, String title, String description, String statusCode, String statusLabel, boolean editable,
                Long departmentId, String departmentCode, String departmentName, Long periodId, String periodName,
                String periodType, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.editable = editable;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public boolean isEditable() { return editable; }
        public Long getDepartmentId() { return departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static final class ProposalFormOptions {
        private final List<DepartmentOption> departments;
        private final List<PeriodOption> periods;

        public ProposalFormOptions(List<DepartmentOption> departments, List<PeriodOption> periods) {
            this.departments = List.copyOf(departments);
            this.periods = List.copyOf(periods);
        }

        public List<DepartmentOption> getDepartments() { return departments; }
        public List<PeriodOption> getPeriods() { return periods; }
    }

    public record DepartmentOption(Long id, String code, String name) {
    }

    public record PeriodOption(Long id, String name, String type) {
    }

    public static final class TopicProposalPage {
        private final List<TopicSummary> topics;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;

        public TopicProposalPage(
                List<TopicSummary> topics, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction) {
            this.topics = List.copyOf(topics);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
        }

        public List<TopicSummary> getTopics() { return topics; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    private record TopicInput(String title, String description) {
    }

    public static class TopicProposalNotFoundException extends RuntimeException {
        public TopicProposalNotFoundException(Long id) {
            super(id == null ? "Topic proposal id is required." : "Topic proposal not found: " + id);
        }
    }

    public static class TopicProposalValidationException extends RuntimeException {
        public TopicProposalValidationException(String message) {
            super(message);
        }
    }
}
