package com.hcmute.topicmanagement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.hcmute.topicmanagement.config.ReportUploadProperties;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.ReportEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.ReportRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.service.LocalReportStorage;
import com.hcmute.topicmanagement.service.ReportService;
import com.hcmute.topicmanagement.service.ReportStorage;

class ReportServiceStorageFailureTest {

    @Test
    void storageFailureDoesNotPersistReportMetadata() {
        UserRepository userRepository = mock(UserRepository.class);
        TopicRegistrationRepository registrationRepository = mock(TopicRegistrationRepository.class);
        ReportRepository reportRepository = mock(ReportRepository.class);
        RegistrationResultRepository registrationResultRepository = mock(RegistrationResultRepository.class);
        ReportStorage reportStorage = mock(ReportStorage.class);

        UserEntity leader = mock(UserEntity.class);
        UserRoleEntity userRole = mock(UserRoleEntity.class);
        RoleEntity studentRole = mock(RoleEntity.class);
        StudentGroupEntity group = mock(StudentGroupEntity.class);
        RegistrationPeriodEntity period = mock(RegistrationPeriodEntity.class);
        TopicRegistrationEntity registration = mock(TopicRegistrationEntity.class);

        when(leader.isActive()).thenReturn(true);
        when(leader.getId()).thenReturn(7L);
        when(leader.getUserRoles()).thenReturn(Set.of(userRole));
        when(userRole.isActive()).thenReturn(true);
        when(userRole.getRole()).thenReturn(studentRole);
        when(studentRole.isActive()).thenReturn(true);
        when(studentRole.getCode()).thenReturn("STUDENT");
        when(userRepository.findByEmailIgnoreCaseWithRolesAndDepartment("leader@student.hcmute.edu.vn"))
                .thenReturn(java.util.Optional.of(leader));
        when(registrationRepository.findApprovedByIdForReadOnly(15L))
                .thenReturn(java.util.Optional.of(registration));
        when(registration.getStudentGroup()).thenReturn(group);
        when(registration.getRegistrationPeriod()).thenReturn(period);
        when(group.getId()).thenReturn(3L);
        when(group.getLeader()).thenReturn(leader);
        when(group.getMembers()).thenReturn(Set.of(leader));
        when(period.getId()).thenReturn(4L);
        when(reportStorage.store(any())).thenThrow(new ReportStorage.ReportStorageException("storage down"));

        ReportService reportService = new ReportService(
                reportRepository, registrationResultRepository, registrationRepository, userRepository, reportStorage,
               new ReportUploadProperties("target/test-report-storage-unit", 1000L, 100, "application/pdf"));

        assertThatThrownBy(() -> reportService.upload(
                "leader@student.hcmute.edu.vn", 3L, 15L, 4L,
                new MockMultipartFile("file", "report.pdf", "application/pdf", "pdf".getBytes())))
                .isInstanceOf(ReportStorage.ReportStorageException.class);
        verify(reportRepository, never()).saveAndFlush(any(ReportEntity.class));
    }
}
