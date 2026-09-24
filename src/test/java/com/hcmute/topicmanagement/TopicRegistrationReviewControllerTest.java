package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TopicRegistrationReviewControllerTest {

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
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicRegistrationRepository topicRegistrationRepository;

    @Test
    void facultyHeadSeesOnlyPendingRegistrationsInTheirDepartment() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("REVIEW-" + suffix);
        DepartmentEntity otherDepartment = department("OTHER-" + suffix);
        UserEntity facultyHead = account("review-head-" + suffix, "Review Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity ownLeader = student("review-own-leader-" + suffix, "Own Leader " + suffix);
        UserEntity otherLeader = student("review-other-leader-" + suffix, "Other Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicRegistrationEntity ownPending = registration(period, ownDepartment, ownLeader,
                "Own registration " + suffix, TopicRegistrationStatus.PENDING, null);
        TopicRegistrationEntity otherPending = registration(period, otherDepartment, otherLeader,
                "Other registration " + suffix, TopicRegistrationStatus.PENDING, null);
        registration(period, ownDepartment, ownLeader, "Already approved " + suffix,
                TopicRegistrationStatus.APPROVED, null);

        mockMvc.perform(get("/faculty/registrations/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/registration-review"))
                .andExpect(content().string(containsString(ownPending.getTopic().getTitle())))
                .andExpect(content().string(containsString("data-registration-approve-form")))
                .andExpect(content().string(containsString("id=\"registration-approve-modal\"")))
                .andExpect(content().string(containsString("Approve registration?")))
                .andExpect(content().string(containsString("data-registration-reject-open")))
                .andExpect(content().string(containsString("id=\"registration-reject-modal\"")))
                .andExpect(content().string(not(containsString(otherPending.getTopic().getTitle()))))
                .andExpect(content().string(not(containsString("Already approved " + suffix))));

        mockMvc.perform(get("/faculty/registrations/review")
                        .param("departmentId", otherDepartment.getId().toString())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(otherPending.getTopic().getTitle()))))
                .andExpect(content().string(containsString("Filter by department")));

        mockMvc.perform(get("/faculty/registrations/review")
                        .param("departmentId", ownDepartment.getId().toString())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ownPending.getTopic().getTitle())))
                .andExpect(content().string(containsString("departmentId=" + ownDepartment.getId())));

        mockMvc.perform(get("/api/faculty/registrations/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].topicTitle").value(hasItem(ownPending.getTopic().getTitle())))
                .andExpect(jsonPath("$[*].topicTitle").value(
                        not(hasItem(otherPending.getTopic().getTitle()))));
    }

    @Test
    void facultyHeadCanApproveAndRejectRegistrations() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("TRANSITION-" + suffix);
        UserEntity facultyHead = account("transition-head-" + suffix, "Transition Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity firstLeader = student("transition-first-leader-" + suffix, "First Leader " + suffix);
        UserEntity secondLeader = student("transition-second-leader-" + suffix, "Second Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicRegistrationEntity approved = registration(period, department, firstLeader,
                "Approve registration " + suffix, TopicRegistrationStatus.PENDING, null);
        TopicRegistrationEntity rejected = registration(period, department, secondLeader,
                "Reject registration " + suffix, TopicRegistrationStatus.PENDING, null);

        mockMvc.perform(post("/faculty/registrations/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .param("registrationId", approved.getId().toString())
                        .param("decision", "APPROVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/registrations/review"))
                .andExpect(flash().attribute("successMessage", "Topic registration was approved."));

        mockMvc.perform(post("/api/registrations/{id}/review", rejected.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"rejectionReason\":\"Topic is not aligned with the period.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Topic is not aligned with the period."));

        Assertions.assertThat(topicRegistrationRepository.findById(approved.getId()).orElseThrow().getStatus())
                .isEqualTo(TopicRegistrationStatus.APPROVED);
        Assertions.assertThat(topicRegistrationRepository.findById(rejected.getId()).orElseThrow().getRejectionReason())
                .isEqualTo("Topic is not aligned with the period.");
    }

    @Test
    void rejectionRequiresAReason() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("REASON-" + suffix);
        UserEntity admin = account("reason-admin-" + suffix, "Reason Admin " + suffix, "ADMIN", null);
        UserEntity leader = student("reason-leader-" + suffix, "Reason Leader " + suffix);
        TopicRegistrationEntity registration = registration(openPeriod(suffix), department, leader,
                "Reason registration " + suffix, TopicRegistrationStatus.PENDING, null);

        mockMvc.perform(post("/api/registrations/{id}/review", registration.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"rejectionReason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_REGISTRATION_REVIEW_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "A rejection reason is required when rejecting a topic registration."));

        Assertions.assertThat(topicRegistrationRepository.findById(registration.getId()).orElseThrow().getStatus())
                .isEqualTo(TopicRegistrationStatus.PENDING);
    }

    @Test
    void approvedAndRejectedRegistrationsCannotBeChanged() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("IMMUTABLE-" + suffix);
        UserEntity admin = account("immutable-admin-" + suffix, "Immutable Admin " + suffix, "ADMIN", null);
        UserEntity firstLeader = student("immutable-first-leader-" + suffix, "First Leader " + suffix);
        UserEntity secondLeader = student("immutable-second-leader-" + suffix, "Second Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicRegistrationEntity approved = registration(period, department, firstLeader,
                "Immutable approved " + suffix, TopicRegistrationStatus.APPROVED, null);
        TopicRegistrationEntity rejected = registration(period, department, secondLeader,
                "Immutable rejected " + suffix, TopicRegistrationStatus.REJECTED, "Already covered.");

        mockMvc.perform(post("/api/registrations/{id}/review", approved.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"rejectionReason\":\"Changed decision.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only pending topic registrations can be approved or rejected."));

        mockMvc.perform(post("/api/registrations/{id}/review", rejected.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only pending topic registrations can be approved or rejected."));

        Assertions.assertThat(topicRegistrationRepository.findByStudentGroup_IdOrderBySubmittedAtDesc(
                        approved.getStudentGroup().getId()))
                .hasSize(1)
                .first()
                .extracting(TopicRegistrationEntity::getStatus)
                .isEqualTo(TopicRegistrationStatus.APPROVED);
        Assertions.assertThat(topicRegistrationRepository.findByStudentGroup_IdOrderBySubmittedAtDesc(
                        rejected.getStudentGroup().getId()))
                .hasSize(1)
                .first()
                .extracting(TopicRegistrationEntity::getRejectionReason)
                .isEqualTo("Already covered.");
    }

    @Test
    void registrationHistoryIsRetainedAndApprovedQueryIsReadOnlyContract() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("HISTORY-" + suffix);
        UserEntity admin = account("history-admin-" + suffix, "History Admin " + suffix, "ADMIN", null);
        UserEntity leader = student("history-leader-" + suffix, "History Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("History Group " + suffix, period, leader, leader));
        TopicRegistrationEntity rejected = registrationForGroup(period, department, group, leader,
                "Historical rejected " + suffix, TopicRegistrationStatus.REJECTED, "Rejected previously.");
        TopicRegistrationEntity pending = registrationForGroup(period, department, group, leader,
                "Current pending " + suffix, TopicRegistrationStatus.PENDING, null);

        Assertions.assertThat(topicRegistrationRepository.findApprovedByIdForReadOnly(pending.getId()))
                .isEmpty();

        mockMvc.perform(post("/api/registrations/{id}/review", pending.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("APPROVED"));

        Assertions.assertThat(topicRegistrationRepository.findByStudentGroup_IdOrderBySubmittedAtDesc(
                        pending.getStudentGroup().getId()))
                .extracting(TopicRegistrationEntity::getId)
                .containsExactlyInAnyOrder(pending.getId(), rejected.getId());
        Assertions.assertThat(topicRegistrationRepository.findApprovedByIdForReadOnly(pending.getId()))
                .isPresent()
                .get()
                .extracting(registrationEntity -> registrationEntity.getStatus())
                .isEqualTo(TopicRegistrationStatus.APPROVED);
    }

    @Test
    void facultyHeadCannotReviewAnotherDepartmentAndStudentsCannotReview() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("SCOPE-" + suffix);
        DepartmentEntity otherDepartment = department("SCOPE-OTHER-" + suffix);
        UserEntity facultyHead = account("scope-head-" + suffix, "Scope Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity student = student("scope-student-" + suffix, "Scope Student " + suffix);
        UserEntity leader = student("scope-leader-" + suffix, "Scope Leader " + suffix);
        TopicRegistrationEntity otherRegistration = registration(openPeriod(suffix), otherDepartment, leader,
                "Other scope registration " + suffix, TopicRegistrationStatus.PENDING, null);

        mockMvc.perform(post("/api/registrations/{id}/review", otherRegistration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOPIC_REGISTRATION_REVIEW_FORBIDDEN"));

        mockMvc.perform(get("/api/registrations/review")
                        .with(user(principal(student.getEmailOrCode(), "ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    private TopicRegistrationEntity registration(
            RegistrationPeriodEntity period, DepartmentEntity department, UserEntity leader, String title,
            TopicRegistrationStatus status, String rejectionReason) {
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Review Group " + suffix(), period, leader, leader));
        return registrationForGroup(period, department, group, leader, title, status, rejectionReason);
    }

    private TopicRegistrationEntity registrationForGroup(
            RegistrationPeriodEntity period, DepartmentEntity department, StudentGroupEntity group,
            UserEntity leader, String title, TopicRegistrationStatus status, String rejectionReason) {
        UserEntity lecturer = account("registration-review-lecturer-" + suffix(),
                "Review Lecturer " + suffix(), "LECTURER", department);
        TopicEntity topic = new TopicEntity(period, department, lecturer, title, "Registration review topic");
        topic.setStatus(TopicStatus.PUBLISHED);
        topic = topicRepository.saveAndFlush(topic);
        TopicRegistrationEntity registration = new TopicRegistrationEntity(group, topic, period, leader);
        registration.setStatus(status);
        registration.setRejectionReason(rejectionReason);
        return topicRegistrationRepository.saveAndFlush(registration);
    }

    private UserEntity account(String login, String fullName, String roleCode, DepartmentEntity department) {
        UserEntity user = new UserEntity(login, fullName, "test-password-hash");
        String domain = "STUDENT".equals(roleCode) ? "@student.hcmute.edu.vn" : "@hcmute.edu.vn";
        user.setEmailOrCode(login.toLowerCase(Locale.ROOT) + domain);
        user.setDepartment(department);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        userRoleRepository.saveAndFlush(new UserRoleEntity(user, role));
        return user;
    }

    private UserEntity student(String login, String fullName) {
        return account(login, fullName, "STUDENT", null);
    }

    private DepartmentEntity department(String code) {
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Review Department " + code));
    }

    private RegistrationPeriodEntity openPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Review Period " + suffix, PeriodType.COURSE,
                now.minusHours(1), now.plusHours(1), now.minusHours(1), now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal adminPrincipal(String email) {
        return principal(email, "ROLE_ADMIN", "REGISTRATION_REVIEW");
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "REGISTRATION_REVIEW");
    }

    private static DatabaseUserPrincipal principal(String email, String role, String... permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new DatabaseUserPrincipal(email, "", "Test user", role, authorities);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
