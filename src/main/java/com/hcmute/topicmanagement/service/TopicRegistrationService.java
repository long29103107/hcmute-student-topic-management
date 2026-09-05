package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
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
    private final UserRepository userRepository;
    private final RegistrationPeriodService registrationPeriodService;

    public TopicRegistrationService(
            TopicRegistrationRepository topicRegistrationRepository,
            StudentGroupRepository studentGroupRepository,
            TopicRepository topicRepository,
            RegistrationPeriodRepository registrationPeriodRepository,
            UserRepository userRepository,
            RegistrationPeriodService registrationPeriodService) {
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.topicRepository = topicRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.userRepository = userRepository;
        this.registrationPeriodService = registrationPeriodService;
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
                .map(TopicRegistrationService::toSummary)
                .toList();
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
        List<PublishedTopicOption> topics = topicRepository.findPublishedForStudent(
                        TopicStatus.PUBLISHED, RegistrationPeriodStatus.OPEN, now).stream()
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

    private static TopicRegistrationSummary toSummary(TopicRegistrationEntity registration) {
        return new TopicRegistrationSummary(
                registration.getId(), registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(), registration.getTopic().getId(),
                registration.getTopic().getTitle(), registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(), registration.getSubmittedBy().getId(),
                registration.getSubmittedBy().getFullName(), registration.getSubmittedBy().getEmailOrCode(),
                registration.getSubmittedAt(), registration.getStatus().name(),
                statusLabel(registration.getStatus()), registration.getRejectionReason());
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

        public TopicRegistrationSummary(
                Long id, Long groupId, String groupName, Long topicId, String topicTitle,
                Long periodId, String periodName, Long submittedById, String submittedByName,
                String submittedByLogin, LocalDateTime submittedAt, String statusCode,
                String statusLabel, String rejectionReason) {
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
