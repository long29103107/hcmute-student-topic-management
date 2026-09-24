package com.hcmute.topicmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.ReportEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReportRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reporttest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "reports.upload.max-size-bytes=8",
        "reports.storage.directory=target/test-report-storage"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportControllerTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-report-storage").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private RegistrationResultRepository registrationResultRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicRegistrationRepository topicRegistrationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReviewBoardRepository reviewBoardRepository;

    @Autowired
    private ReviewBoardMemberRepository reviewBoardMemberRepository;

    @AfterEach
    void cleanStoredReports() throws Exception {
        for (ReportEntity report : reportRepository.findAll()) {
            Files.deleteIfExists(STORAGE_ROOT.resolve(report.getStoredName()));
        }
        reportRepository.deleteAll();
        if (Files.exists(STORAGE_ROOT)) {
            try (var files = Files.list(STORAGE_ROOT)) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Test cleanup must not hide the assertion failure.
                    }
                });
            }
        }
    }

    @Test
    void approvedRegistrationLeaderCanUploadAndMetadataIsPersisted() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-leader-" + suffix, "Report Leader " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        MockMultipartFile file = pdf("report.pdf", "pdf-data");
        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(file)
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(registration.getId()))
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.fileSize").value(8))
                .andExpect(jsonPath("$.storedName").value(org.hamcrest.Matchers.not("report.pdf")));

        ReportEntity persisted = reportRepository
                .findFirstByTopicRegistration_IdOrderBySubmittedAtDesc(registration.getId())
                .orElseThrow();
        assertThat(persisted.getOriginalName()).isEqualTo("report.pdf");
        assertThat(persisted.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(persisted.getFileSize()).isEqualTo(8L);
        assertThat(persisted.getUploader().getId()).isEqualTo(leader.getId());
        assertThat(persisted.getStoredName()).doesNotContain("report.pdf", "/", "\\");
        assertThat(Files.exists(STORAGE_ROOT.resolve(persisted.getStoredName()))).isTrue();
    }

    @Test
    void nonLeaderCannotUploadAndRelationshipMismatchIsRejected() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-owner-" + suffix, "Report Owner " + suffix);
        UserEntity member = student("report-member-" + suffix, "Report Member " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        group.getMembers().add(member);
        group = studentGroupRepository.saveAndFlush(group);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(member.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_FORBIDDEN"));

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", String.valueOf(period.getId() + 1000))
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_FORBIDDEN"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void pendingRegistrationAndInvalidFilesAreRejectedBeforeStorage() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-invalid-" + suffix, "Report Invalid " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.PENDING);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));

        TopicRegistrationEntity approved = registration(period, group, leader, TopicRegistrationStatus.APPROVED);
        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), approved.getId())
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "pdf-data".getBytes()))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_INVALID"));

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), approved.getId())
                        .file(pdf("large.pdf", "too-large"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_INVALID"));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void studentRegistrationPageRendersUploadFormForApprovedRegistration() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-page-" + suffix, "Report Page " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(get("/student/registrations")
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString(
                        "/student/registrations/" + registration.getId() + "/report")))
                .andExpect(content().string(containsString("Upload report")));
    }

    @Test
    void studentRegistrationPageHidesUploadFormAfterResultPublication() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-published-page-" + suffix, "Report Published Page " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);
        RegistrationResultEntity result = new RegistrationResultEntity(registration);
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        registrationResultRepository.saveAndFlush(result);

        mockMvc.perform(get("/student/registrations")
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Upload report"))))
                .andExpect(content().string(containsString("Upload closed after result publication")))
                .andExpect(content().string(not(containsString("data-report-upload-open"))));
    }

    @Test
    void publishedResultBlocksReportUploadEndpoint() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-published-upload-" + suffix, "Report Published Upload " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);
        RegistrationResultEntity result = new RegistrationResultEntity(registration);
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        registrationResultRepository.saveAndFlush(result);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "Reports cannot be changed after the result is published."));

        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void reportUploadRequiresCsrf() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-csrf-" + suffix, "Report CSRF " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportMetadataAndDownloadAreLimitedToRelatedGroupMembers() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-view-leader-" + suffix, "Report View Leader " + suffix);
        UserEntity member = student("report-view-member-" + suffix, "Report View Member " + suffix);
        UserEntity outsider = student("report-view-outsider-" + suffix, "Report View Outsider " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        group.getMembers().add(member);
        studentGroupRepository.saveAndFlush(group);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isCreated());
        ReportEntity report = reportRepository
                .findFirstByTopicRegistration_IdOrderBySubmittedAtDesc(registration.getId())
                .orElseThrow();

        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(studentViewerPrincipal(member.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.getId()))
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.storedName").doesNotExist());

        mockMvc.perform(get("/api/reports/{reportId}/download", report.getId())
                        .with(user(studentViewerPrincipal(member.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("pdf-data".getBytes()))
                .andExpect(header().string("Content-Disposition", containsString("report.pdf")));

        mockMvc.perform(get("/reports/view")
                        .param("id", report.getId().toString())
                        .with(user(studentViewerPrincipal(member.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().bytes("pdf-data".getBytes()));

        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(studentViewerPrincipal(outsider.getEmailOrCode()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_FORBIDDEN"));

        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(studentPrincipal(member.getEmailOrCode()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportViewAllowsSupervisorEvaluatorAndFacultyHeadWithinTheirScope() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("report-scope-leader-" + suffix, "Report Scope Leader " + suffix);
        RegistrationPeriodEntity period = period(suffix);
        StudentGroupEntity group = group(period, leader);
        TopicRegistrationEntity registration = registration(period, group, leader, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(multipart("/api/student/groups/{groupId}/registrations/{registrationId}/reports",
                        group.getId(), registration.getId())
                        .file(pdf("report.pdf", "pdf-data"))
                        .param("periodId", period.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isCreated());
        ReportEntity report = reportRepository
                .findFirstByTopicRegistration_IdOrderBySubmittedAtDesc(registration.getId())
                .orElseThrow();

        UserEntity supervisor = account("report-supervisor-" + suffix, "Report Supervisor " + suffix, "LECTURER");
        registration.getTopic().getSupervisors().add(supervisor);
        topicRepository.saveAndFlush(registration.getTopic());
        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(lecturerViewerPrincipal(supervisor.getEmailOrCode()))))
                .andExpect(status().isOk());

        UserEntity evaluator = account("report-evaluator-" + suffix, "Report Evaluator " + suffix, "LECTURER");
        ReviewBoardEntity board = reviewBoardRepository.saveAndFlush(new ReviewBoardEntity(registration, evaluator));
        reviewBoardMemberRepository.saveAndFlush(new ReviewBoardMemberEntity(board, evaluator));
        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(lecturerViewerPrincipal(evaluator.getEmailOrCode()))))
                .andExpect(status().isOk());

        UserEntity facultyHead = account("report-head-" + suffix, "Report Head " + suffix, "FACULTY_HEAD");
        facultyHead.setDepartment(registration.getTopic().getDepartment());
        userRepository.saveAndFlush(facultyHead);
        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(facultyHeadViewerPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk());

        UserEntity otherHead = account("report-other-head-" + suffix, "Report Other Head " + suffix, "FACULTY_HEAD");
        otherHead.setDepartment(department("OTHER-" + suffix));
        userRepository.saveAndFlush(otherHead);
        mockMvc.perform(get("/api/reports/{reportId}", report.getId())
                        .with(user(facultyHeadViewerPrincipal(otherHead.getEmailOrCode()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_FORBIDDEN"));
    }

    private TopicRegistrationEntity registration(
            RegistrationPeriodEntity period, StudentGroupEntity group, UserEntity leader,
            TopicRegistrationStatus status) {
        DepartmentEntity department = department("REPORT-" + suffix());
        UserEntity lecturer = account("report-lecturer-" + suffix(), "Report Lecturer " + suffix(), "LECTURER");
        TopicEntity topic = new TopicEntity(
                period, department, lecturer, "Report topic " + suffix(), "Report topic description");
        topic.setStatus(TopicStatus.PUBLISHED);
        topic = topicRepository.saveAndFlush(topic);
        TopicRegistrationEntity registration = new TopicRegistrationEntity(group, topic, period, leader);
        registration.setStatus(status);
        return topicRegistrationRepository.saveAndFlush(registration);
    }

    private StudentGroupEntity group(RegistrationPeriodEntity period, UserEntity leader) {
        return studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Report group " + suffix(), period, leader, leader));
    }

    private RegistrationPeriodEntity period(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Report period " + suffix, PeriodType.COURSE,
                now.minusDays(1), now.plusDays(1), now.minusDays(1), now.plusDays(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private DepartmentEntity department(String code) {
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Report Department " + code));
    }

    private UserEntity student(String login, String fullName) {
        return account(login, fullName, "STUDENT");
    }

    private UserEntity account(String login, String fullName, String roleCode) {
        UserEntity user = new UserEntity(login, fullName, "test-password-hash");
        String domain = "STUDENT".equals(roleCode) ? "@student.hcmute.edu.vn" : "@hcmute.edu.vn";
        user.setEmailOrCode(login.toLowerCase(Locale.ROOT) + domain);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        userRoleRepository.saveAndFlush(new UserRoleEntity(user, role));
        return user;
    }

    private static MockMultipartFile pdf(String name, String body) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, body.getBytes());
    }

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Report student", "Student",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("REPORT_SUBMIT"),
                        new SimpleGrantedAuthority("REGISTRATION_SUBMIT")));
    }

    private static DatabaseUserPrincipal studentViewerPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Report viewer", "Student",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("REPORT_VIEW")));
    }

    private static DatabaseUserPrincipal lecturerViewerPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Lecturer viewer", "Lecturer",
                List.of(new SimpleGrantedAuthority("ROLE_LECTURER"),
                        new SimpleGrantedAuthority("REPORT_VIEW")));
    }

    private static DatabaseUserPrincipal facultyHeadViewerPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Faculty head viewer", "Faculty Head",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY_HEAD"),
                        new SimpleGrantedAuthority("REPORT_VIEW")));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
