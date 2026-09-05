package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Locale;

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

    public record ReportSummary(
            Long id, Long registrationId, Long groupId, String originalName, String storedName,
            String contentType, Long fileSize, Long uploaderId, String uploaderName,
            LocalDateTime submittedAt) {
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
