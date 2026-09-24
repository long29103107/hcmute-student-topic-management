package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReportRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicRegistrationService {

    private static final Collection<TopicRegistrationStatus> CURRENT_STATUSES = List.of(
            TopicRegistrationStatus.PENDING, TopicRegistrationStatus.APPROVED);

    private final TopicRegistrationRepository topicRegistrationRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final TopicRepository topicRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodService registrationPeriodService;
    private final ReportRepository reportRepository;

    public TopicRegistrationService(
            TopicRegistrationRepository topicRegistrationRepository,
            StudentGroupRepository studentGroupRepository,
            TopicRepository topicRepository,
            RegistrationPeriodRepository registrationPeriodRepository,
            RegistrationResultRepository registrationResultRepository,
            UserRepository userRepository,
            RegistrationPeriodService registrationPeriodService,
            ReportRepository reportRepository) {
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.topicRepository = topicRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.userRepository = userRepository;
        this.registrationPeriodService = registrationPeriodService;
        this.reportRepository = reportRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public TopicRegistrationSummary submit(
            String studentEmail, Long groupId, Long selectedPeriodId, Long topicId) {
        UserEntity student = findActiveStudent(studentEmail);
        StudentGroupEntity group = findGroupForMutation(groupId);
        ensureActiveGroup(group);
        ensureLeader(student, group);

        Long groupPeriodId = group.getRegistrationPeriod().getId();
        Long effectivePeriodId = selectedPeriodId == null ? groupPeriodId : selectedPeriodId;
        if (!groupPeriodId.equals(effectivePeriodId)) {
            throw new TopicRegistrationValidationException(
                    "The selected registration period does not match the group.");
        }

        RegistrationPeriodEntity period = lockPeriod(groupPeriodId);
        requireOpenStudentWindow(period.getId());

        TopicEntity topic = topicRepository.findByIdForRegistration(topicId)
                .orElseThrow(() -> new TopicRegistrationNotFoundException(
                        "Published topic not found: " + topicId));
        if (topic.getStatus() != TopicStatus.PUBLISHED) {
            throw new TopicRegistrationValidationException(
                    "Only published topics can be registered.");
        }
        if (topic.getRegistrationPeriod() == null
                || !period.getId().equals(topic.getRegistrationPeriod().getId())) {
            throw new TopicRegistrationValidationException(
                    "The selected topic must belong to the group's registration period.");
        }
        if (student.getDepartment() == null || topic.getDepartment() == null
                || !student.getDepartment().getId().equals(topic.getDepartment().getId())) {
            throw new TopicRegistrationValidationException(
                    "The selected topic must belong to the student's department.");
        }
        if (topicRegistrationRepository.existsByStudentGroup_IdAndRegistrationPeriod_IdAndStatusIn(
                group.getId(), period.getId(), CURRENT_STATUSES)) {
            throw new TopicRegistrationValidationException(
                    "This group already has a current topic registration in this period.");
        }

        TopicRegistrationEntity registration = new TopicRegistrationEntity(group, topic, period, student);
        return toSummary(topicRegistrationRepository.saveAndFlush(registration));
    }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public List<TopicRegistrationSummary> listForStudent(String studentEmail) {
        UserEntity student = findActiveStudent(studentEmail);
        return topicRegistrationRepository.findForStudentWithDetails(student.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

        @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
        public TopicRegistrationPage listForStudentPage(
            String studentEmail, String search, String status,
            int page, int size, String sort, String direction) {
        UserEntity student = findActiveStudent(studentEmail);
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        List<TopicRegistrationSummary> filtered = topicRegistrationRepository
            .findForStudentWithDetails(student.getId()).stream()
            .map(this::toSummary)
            .filter(registration -> normalizedStatus.isBlank()
                || normalizedStatus.equals(registration.getStatusCode()))
            .filter(registration -> matchesSearch(registration, normalizedSearch))
            .sorted(registrationComparator(normalizedSort, normalizedDirection))
            .toList();
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new TopicRegistrationPage(
            filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
            normalizedSearch, normalizedStatus, normalizedSort, normalizedDirection);
        }

    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public TopicRegistrationForm registrationForm(String studentEmail, Long groupId) {
        UserEntity student = findActiveStudent(studentEmail);
        StudentGroupEntity group = studentGroupRepository.findByIdWithDetails(groupId)
                .orElseThrow(() -> new TopicRegistrationNotFoundException(
                        "Student group not found: " + groupId));
        ensureActiveGroup(group);
        ensureLeader(student, group);

        RegistrationPeriodEntity period = group.getRegistrationPeriod();
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodService.PeriodAccess periodAccess = registrationPeriodService.inspect(
                period.getId(), now);
        Long departmentId = student.getDepartment() == null ? null : student.getDepartment().getId();
        List<PublishedTopicOption> topics = departmentId == null ? List.of()
                : topicRepository.findPublishedForStudentByDepartment(
                        TopicStatus.PUBLISHED, RegistrationPeriodStatus.OPEN, departmentId, now).stream()
                .filter(topic -> period.getId().equals(topic.getRegistrationPeriod().getId()))
                .map(TopicRegistrationService::toTopicOption)
                .toList();
        return new TopicRegistrationForm(
                group.getId(), group.getName(), period.getId(), period.getName(), period.getType().name(),
                periodAccess.isStudentRegistrationOpen(), period.getStudentRegistrationEnd(), topics);
    }

    private UserEntity findActiveStudent(String email) {
        UserEntity student = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new TopicRegistrationAccessException(
                        "Student account is not available."));
        boolean studentRole = student.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive()
                        && "STUDENT".equalsIgnoreCase(role.getCode()));
        if (!studentRole) {
            throw new TopicRegistrationAccessException(
                    "Only active students can submit topic registrations.");
        }
        return student;
    }

    private StudentGroupEntity findGroupForMutation(Long groupId) {
        if (groupId == null) {
            throw new TopicRegistrationValidationException("Group id is required.");
        }
        return studentGroupRepository.findByIdForMutation(groupId)
                .orElseThrow(() -> new TopicRegistrationNotFoundException(
                        "Student group not found: " + groupId));
    }

    private RegistrationPeriodEntity lockPeriod(Long periodId) {
        return registrationPeriodRepository.findByIdForGroupMutation(periodId)
                .orElseThrow(() -> new TopicRegistrationNotFoundException(
                        "Registration period not found: " + periodId));
    }

    private void requireOpenStudentWindow(Long periodId) {
        try {
            registrationPeriodService.requireOpenForStudent(periodId, LocalDateTime.now());
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException exception) {
            throw new TopicRegistrationNotFoundException("Registration period not found: " + periodId);
        } catch (RegistrationPeriodService.RegistrationPeriodAccessException exception) {
            throw new TopicRegistrationValidationException(switch (exception.getCode()) {
                case RegistrationPeriodService.PERIOD_NOT_OPEN ->
                        "The registration period is not open for student registrations.";
                case RegistrationPeriodService.STUDENT_WINDOW_CLOSED ->
                        "The student registration window is closed.";
                default -> exception.getMessage();
            });
        }
    }

    private static void ensureActiveGroup(StudentGroupEntity group) {
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new TopicRegistrationValidationException(
                    "Only active groups can submit topic registrations.");
        }
    }

    private static void ensureLeader(UserEntity student, StudentGroupEntity group) {
        boolean isLeader = group.getLeader() != null
                && student.getId().equals(group.getLeader().getId())
                && group.getMembers().stream().anyMatch(member -> student.getId().equals(member.getId()));
        if (!isLeader) {
            throw new TopicRegistrationAccessException(
                    "Only the current group leader can submit a topic registration.");
        }
    }

    private TopicRegistrationSummary toSummary(TopicRegistrationEntity registration) {
        return new TopicRegistrationSummary(
                registration.getId(), registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(), registration.getTopic().getId(),
                registration.getTopic().getTitle(), registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(), registration.getSubmittedBy().getId(),
                registration.getSubmittedBy().getFullName(), registration.getSubmittedBy().getEmailOrCode(),
                registration.getSubmittedAt(), registration.getStatus().name(),
                statusLabel(registration.getStatus()), registration.getRejectionReason(),
                registrationResultRepository.findByTopicRegistration_Id(registration.getId())
                        .map(result -> result.getStatus() == RegistrationResultStatus.PUBLISHED)
                        .orElse(false),
                reportRepository.findByTopicRegistration_IdOrderBySubmittedAtDesc(registration.getId()).stream()
                        .map(report -> new ReportFileSummary(
                                report.getId(), report.getOriginalName(), report.getFileSize(), report.getSubmittedAt()))
                        .toList());
    }

    private static PublishedTopicOption toTopicOption(TopicEntity topic) {
        return new PublishedTopicOption(
                topic.getId(), topic.getTitle(), topic.getDescription(), topic.getDepartment().getCode(),
                topic.getDepartment().getName(), topic.getRegistrationPeriod().getId());
    }

    private static String statusLabel(TopicRegistrationStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private static boolean matchesSearch(TopicRegistrationSummary registration, String search) {
        return search.isBlank()
                || containsIgnoreCase(registration.getGroupName(), search)
                || containsIgnoreCase(registration.getTopicTitle(), search)
                || containsIgnoreCase(registration.getPeriodName(), search)
                || containsIgnoreCase(registration.getSubmittedByName(), search)
                || containsIgnoreCase(registration.getSubmittedByLogin(), search)
                || containsIgnoreCase(registration.getStatusLabel(), search)
                || containsIgnoreCase(registration.getRejectionReason(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<TopicRegistrationSummary> registrationComparator(String sort, String direction) {
        Comparator<TopicRegistrationSummary> comparator = switch (sort) {
            case "group" -> Comparator.comparing(
                    TopicRegistrationSummary::getGroupName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicRegistrationSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "period" -> Comparator.comparing(
                    TopicRegistrationSummary::getPeriodName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicRegistrationSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "submitted" -> Comparator.comparing(
                    TopicRegistrationSummary::getSubmittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TopicRegistrationSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(
                    TopicRegistrationSummary::getStatusLabel, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicRegistrationSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    TopicRegistrationSummary::getTopicTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(TopicRegistrationSummary::getGroupName, String.CASE_INSENSITIVE_ORDER);
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        try {
            return TopicRegistrationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "group", "period", "submitted", "status" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "topic";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    public static final class TopicRegistrationSummary {
        private final Long id;
        private final Long groupId;
        private final String groupName;
        private final Long topicId;
        private final String topicTitle;
        private final Long periodId;
        private final String periodName;
        private final Long submittedById;
        private final String submittedByName;
        private final String submittedByLogin;
        private final LocalDateTime submittedAt;
        private final String statusCode;
        private final String statusLabel;
        private final String rejectionReason;
        private final boolean resultPublished;
        private final List<ReportFileSummary> reportFiles;

        public TopicRegistrationSummary(
                Long id, Long groupId, String groupName, Long topicId, String topicTitle,
                Long periodId, String periodName, Long submittedById, String submittedByName,
                String submittedByLogin, LocalDateTime submittedAt, String statusCode,
                String statusLabel, String rejectionReason, boolean resultPublished,
                List<ReportFileSummary> reportFiles) {
            this.id = id;
            this.groupId = groupId;
            this.groupName = groupName;
            this.topicId = topicId;
            this.topicTitle = topicTitle;
            this.periodId = periodId;
            this.periodName = periodName;
            this.submittedById = submittedById;
            this.submittedByName = submittedByName;
            this.submittedByLogin = submittedByLogin;
            this.submittedAt = submittedAt;
            this.statusCode = statusCode;
            this.statusLabel = statusLabel;
            this.rejectionReason = rejectionReason;
            this.resultPublished = resultPublished;
            this.reportFiles = List.copyOf(reportFiles);
        }

        public Long getId() { return id; }
        public Long getGroupId() { return groupId; }
        public String getGroupName() { return groupName; }
        public Long getTopicId() { return topicId; }
        public String getTopicTitle() { return topicTitle; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public Long getSubmittedById() { return submittedById; }
        public String getSubmittedByName() { return submittedByName; }
        public String getSubmittedByLogin() { return submittedByLogin; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public String getStatusCode() { return statusCode; }
        public String getStatusLabel() { return statusLabel; }
        public String getRejectionReason() { return rejectionReason; }
        public boolean isResultPublished() { return resultPublished; }
        public List<ReportFileSummary> getReportFiles() { return reportFiles; }
    }

    public static final class ReportFileSummary {
        private final Long id;
        private final String originalName;
        private final Long fileSize;
        private final LocalDateTime submittedAt;

        public ReportFileSummary(Long id, String originalName, Long fileSize, LocalDateTime submittedAt) {
            this.id = id;
            this.originalName = originalName;
            this.fileSize = fileSize;
            this.submittedAt = submittedAt;
        }

        public Long getId() { return id; }
        public String getOriginalName() { return originalName; }
        public Long getFileSize() { return fileSize; }
        public LocalDateTime getSubmittedAt() { return submittedAt; }
    }

    public static final class TopicRegistrationPage {
        private final List<TopicRegistrationSummary> registrations;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String status;
        private final String sort;
        private final String direction;

        public TopicRegistrationPage(
                List<TopicRegistrationSummary> registrations, int page, int size, int totalItems,
                int totalPages, String search, String status, String sort, String direction) {
            this.registrations = List.copyOf(registrations);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.status = status;
            this.sort = sort;
            this.direction = direction;
        }

        public List<TopicRegistrationSummary> getRegistrations() { return registrations; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getStatus() { return status; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static final class TopicRegistrationForm {
        private final Long groupId;
        private final String groupName;
        private final Long periodId;
        private final String periodName;
        private final String periodType;
        private final boolean studentRegistrationOpen;
        private final LocalDateTime studentRegistrationEnd;
        private final List<PublishedTopicOption> topics;

        public TopicRegistrationForm(
                Long groupId, String groupName, Long periodId, String periodName, String periodType,
                boolean studentRegistrationOpen, LocalDateTime studentRegistrationEnd,
                List<PublishedTopicOption> topics) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.periodId = periodId;
            this.periodName = periodName;
            this.periodType = periodType;
            this.studentRegistrationOpen = studentRegistrationOpen;
            this.studentRegistrationEnd = studentRegistrationEnd;
            this.topics = List.copyOf(topics);
        }

        public Long getGroupId() { return groupId; }
        public String getGroupName() { return groupName; }
        public Long getPeriodId() { return periodId; }
        public String getPeriodName() { return periodName; }
        public String getPeriodType() { return periodType; }
        public boolean isStudentRegistrationOpen() { return studentRegistrationOpen; }
        public LocalDateTime getStudentRegistrationEnd() { return studentRegistrationEnd; }
        public List<PublishedTopicOption> getTopics() { return topics; }
    }

    public record PublishedTopicOption(
            Long id, String title, String description, String departmentCode,
            String departmentName, Long periodId) {
    }

    public static class TopicRegistrationNotFoundException extends RuntimeException {
        public TopicRegistrationNotFoundException(String message) {
            super(message);
        }
    }

    public static class TopicRegistrationValidationException extends RuntimeException {
        public TopicRegistrationValidationException(String message) {
            super(message);
        }
    }

    public static class TopicRegistrationAccessException extends RuntimeException {
        public TopicRegistrationAccessException(String message) {
            super(message);
        }
    }
}
