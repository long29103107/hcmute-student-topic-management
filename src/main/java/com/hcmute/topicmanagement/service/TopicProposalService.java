package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TopicProposalService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;
    private static final Set<TopicStatus> EDITABLE_STATUSES =
            EnumSet.of(TopicStatus.DRAFT, TopicStatus.REJECTED);

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;

    public TopicProposalService(
            TopicRepository topicRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RegistrationPeriodRepository registrationPeriodRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
    }

    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public List<TopicSummary> listOwnProposals(String lecturerEmail) {
        UserEntity lecturer = findActiveLecturer(lecturerEmail);
        return topicRepository.findOwnProposalsWithPeriodAndDepartment(lecturer.getId()).stream()
                .map(TopicProposalService::toSummary)
                .toList();
    }

    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public ProposalFormOptions getProposalFormOptions() {
        LocalDateTime now = LocalDateTime.now();
        List<DepartmentOption> departments = departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(department -> new DepartmentOption(
                        department.getId(), department.getCode(), department.getName()))
                .toList();
        List<PeriodOption> periods = registrationPeriodRepository
                .findOpenForLecturer(RegistrationPeriodStatus.OPEN, now).stream()
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
        return registrationPeriodRepository.findById(periodId)
                .filter(period -> period.getStatus() == RegistrationPeriodStatus.OPEN)
                .filter(period -> isWithinLecturerWindow(period, now))
                .orElseThrow(() -> new TopicProposalValidationException(
                        "The selected registration period is not open for lecturer proposals."));
    }

    private static boolean isWithinLecturerWindow(RegistrationPeriodEntity period, LocalDateTime now) {
        return !now.isBefore(period.getLecturerRegistrationStart())
                && !now.isAfter(period.getLecturerRegistrationEnd());
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
