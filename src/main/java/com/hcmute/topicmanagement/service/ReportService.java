package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.hcmute.topicmanagement.config.ReportUploadProperties;
import com.hcmute.topicmanagement.model.ReportEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.ReportRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;
    private final UserRepository userRepository;
    private final ReportStorage reportStorage;
    private final ReportUploadProperties uploadProperties;

    public ReportService(
            ReportRepository reportRepository,
            TopicRegistrationRepository topicRegistrationRepository,
            UserRepository userRepository,
            ReportStorage reportStorage,
            ReportUploadProperties uploadProperties) {
        this.reportRepository = reportRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.userRepository = userRepository;
        this.reportStorage = reportStorage;
        this.uploadProperties = uploadProperties;
    }

    @Transactional
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REPORT_SUBMIT')")
    public ReportSummary upload(
            String studentEmail, Long groupId, Long registrationId, Long periodId, MultipartFile file) {
        UserEntity student = findActiveStudent(studentEmail);
        validateIds(groupId, registrationId, periodId);

        TopicRegistrationEntity registration = topicRegistrationRepository
                .findApprovedByIdForReadOnly(registrationId)
                .orElseThrow(() -> new ReportNotFoundException(
                        "Approved topic registration not found: " + registrationId));
        StudentGroupEntity group = registration.getStudentGroup();
        if (!groupId.equals(group.getId()) || !periodId.equals(registration.getRegistrationPeriod().getId())) {
            throw new ReportAccessException(
                    "The registration does not belong to the requested group and period.");
        }
        ensureCurrentGroupLeader(student, group);
        validateFile(file);

        ReportStorage.StoredReport storedReport = reportStorage.store(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        String contentType = normalizedContentType(file.getContentType());
        ReportEntity report = new ReportEntity(
                registration, storedReport.storedName(), originalName, contentType, file.getSize(), student);
        try {
            return toSummary(reportRepository.saveAndFlush(report));
        } catch (RuntimeException | Error exception) {
            cleanupAfterPersistenceFailure(storedReport.storedName(), exception);
            throw exception;
        }
    }

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ReportAccessSummary metadata(String actorEmail, Long reportId) {
        ReportEntity report = findReport(reportId);
        authorizeView(findActiveActor(actorEmail), report);
        return toAccessSummary(report);
    }

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ReportDownload openDownload(String actorEmail, Long reportId) {
        ReportEntity report = findReport(reportId);
        authorizeView(findActiveActor(actorEmail), report);
        ReportStorage.StoredReportContent content = reportStorage.open(report.getStoredName());
        return new ReportDownload(toAccessSummary(report), content.content(), content.contentLength());
    }

    private UserEntity findActiveStudent(String email) {
        UserEntity student = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new ReportAccessException("Student account is not available."));
        boolean studentRole = student.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive()
                        && "STUDENT".equalsIgnoreCase(role.getCode()));
        if (!studentRole) {
            throw new ReportAccessException("Only active students can submit reports.");
        }
        return student;
    }

    private UserEntity findActiveActor(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(this::hasActiveRole)
                .orElseThrow(() -> new ReportAccessException("Report viewer account is not available."));
    }

    private ReportEntity findReport(Long reportId) {
        if (reportId == null) {
            throw new ReportValidationException("Report id is required.");
        }
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));
    }

    private void authorizeView(UserEntity actor, ReportEntity report) {
        TopicRegistrationEntity registration = report.getTopicRegistration();
        StudentGroupEntity group = registration.getStudentGroup();

        if (hasActiveRole(actor, "ADMIN")) {
            return;
        }
        if (hasActiveRole(actor, "STUDENT") && isGroupMember(actor, group)) {
            return;
        }
        if (hasActiveRole(actor, "FACULTY_HEAD") && isSameDepartment(actor, registration)) {
            return;
        }
        if (hasActiveRole(actor, "LECTURER")
                && (isTopicSupervisor(actor, registration) || isAssignedEvaluator(actor, registration))) {
            return;
        }
        throw new ReportAccessException("You are not allowed to view this report.");
    }

    private boolean hasActiveRole(UserEntity actor) {
        return actor.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive());
    }

    private static boolean hasActiveRole(UserEntity actor, String roleCode) {
        return actor.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive()
                        && roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean isGroupMember(UserEntity actor, StudentGroupEntity group) {
        return actor.getId() != null && group.getMembers().stream()
                .anyMatch(member -> Objects.equals(actor.getId(), member.getId()));
    }

    private static boolean isSameDepartment(UserEntity actor, TopicRegistrationEntity registration) {
        return actor.getDepartment() != null
                && registration.getTopic().getDepartment() != null
                && Objects.equals(actor.getDepartment().getId(), registration.getTopic().getDepartment().getId());
    }

    private static boolean isTopicSupervisor(UserEntity actor, TopicRegistrationEntity registration) {
        return actor.getId() != null && registration.getTopic().getSupervisors().stream()
                .anyMatch(supervisor -> Objects.equals(actor.getId(), supervisor.getId()));
    }

    private static boolean isAssignedEvaluator(UserEntity actor, TopicRegistrationEntity registration) {
        if (actor.getId() == null) {
            return false;
        }
        if (registration.getReviewBoard() != null && registration.getReviewBoard().getMembers().stream()
                .anyMatch(member -> member.getLecturer() != null
                        && Objects.equals(actor.getId(), member.getLecturer().getId()))) {
            return true;
        }
        return registration.getEvaluations().stream()
                .anyMatch(evaluation -> evaluation.getLecturer() != null
                        && Objects.equals(actor.getId(), evaluation.getLecturer().getId()));
    }

    private static void validateIds(Long groupId, Long registrationId, Long periodId) {
        if (groupId == null || registrationId == null || periodId == null) {
            throw new ReportValidationException("Group, registration and period ids are required.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ReportValidationException("A non-empty report file is required.");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new ReportValidationException("The report file name is required.");
        }
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new ReportValidationException("The report file exceeds the configured size limit.");
        }
        String contentType = normalizedContentType(file.getContentType());
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new ReportValidationException("The report file type is not allowed.");
        }
    }

    private static void ensureCurrentGroupLeader(UserEntity student, StudentGroupEntity group) {
        boolean isLeader = group.getLeader() != null
                && student.getId().equals(group.getLeader().getId())
                && group.getMembers().stream().anyMatch(member -> student.getId().equals(member.getId()));
        if (!isLeader) {
            throw new ReportAccessException("Only the current group leader can submit a report.");
        }
    }

    private static String safeOriginalName(String originalName) {
        String normalized = originalName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return normalized.substring(lastSlash + 1).trim();
    }

    private static String normalizedContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim().toLowerCase(Locale.ROOT) : null;
    }

    private void cleanupAfterPersistenceFailure(String storedName, Throwable failure) {
        try {
            reportStorage.delete(storedName);
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private static ReportSummary toSummary(ReportEntity report) {
        return new ReportSummary(
                report.getId(), report.getTopicRegistration().getId(),
                report.getTopicRegistration().getStudentGroup().getId(), report.getOriginalName(),
                report.getStoredName(), report.getContentType(), report.getFileSize(),
                report.getUploader().getId(), report.getUploader().getFullName(), report.getSubmittedAt());
    }

    private static ReportAccessSummary toAccessSummary(ReportEntity report) {
        TopicRegistrationEntity registration = report.getTopicRegistration();
        return new ReportAccessSummary(
                report.getId(), registration.getId(), registration.getStudentGroup().getId(),
                registration.getStudentGroup().getName(), registration.getTopic().getId(),
                registration.getTopic().getTitle(), registration.getRegistrationPeriod().getId(),
                registration.getRegistrationPeriod().getName(), report.getOriginalName(),
                report.getContentType(), report.getFileSize(), report.getUploader().getId(),
                report.getUploader().getFullName(), report.getSubmittedAt());
    }

    public record ReportSummary(
            Long id, Long registrationId, Long groupId, String originalName, String storedName,
            String contentType, Long fileSize, Long uploaderId, String uploaderName,
            LocalDateTime submittedAt) {
    }

    public record ReportAccessSummary(
            Long id, Long registrationId, Long groupId, String groupName, Long topicId,
            String topicTitle, Long periodId, String periodName, String originalName,
            String contentType, Long fileSize, Long uploaderId, String uploaderName,
            LocalDateTime submittedAt) {
    }

    public record ReportDownload(
            ReportAccessSummary metadata, java.io.InputStream content, long contentLength) {
    }

    public static class ReportNotFoundException extends RuntimeException {
        public ReportNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReportValidationException extends RuntimeException {
        public ReportValidationException(String message) {
            super(message);
        }
    }

    public static class ReportAccessException extends RuntimeException {
        public ReportAccessException(String message) {
            super(message);
        }
    }
}
