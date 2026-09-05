package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.hcmute.topicmanagement.repository.EvaluationRepository;
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
class EvaluatorAssignmentControllerTest {

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

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Test
    void facultyHeadCanAssignAndChangeEvaluatorWithoutDuplicateRows() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("EVAL-" + suffix);
        UserEntity facultyHead = account("eval-head-" + suffix, "Evaluator Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity firstEvaluator = account("eval-first-" + suffix, "First Evaluator " + suffix,
                "LECTURER", department);
        UserEntity secondEvaluator = account("eval-second-" + suffix, "Second Evaluator " + suffix,
                "FACULTY_HEAD", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Assignment " + suffix, TopicRegistrationStatus.APPROVED);

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", registration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + firstEvaluator.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registration.getId()))
                .andExpect(jsonPath("$.assignedEvaluator.id").value(firstEvaluator.getId()));

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", registration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + secondEvaluator.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedEvaluator.id").value(secondEvaluator.getId()));

        Assertions.assertThat(evaluationRepository.findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId()))
                .hasSize(1)
                .first()
                .extracting(evaluation -> evaluation.getLecturer().getId())
                .isEqualTo(secondEvaluator.getId());
    }

    @Test
    void assignmentRejectsSupervisorAndUsersWithoutEvaluatorRole() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("EVAL-RULES-" + suffix);
        UserEntity facultyHead = account("eval-rules-head-" + suffix, "Rules Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity supervisor = account("eval-supervisor-" + suffix, "Supervisor " + suffix,
                "LECTURER", department);
        UserEntity student = account("eval-student-" + suffix, "Student " + suffix, "STUDENT", null);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Rule registration " + suffix, TopicRegistrationStatus.APPROVED);
        registration.getTopic().getSupervisors().add(supervisor);
        topicRepository.saveAndFlush(registration.getTopic());

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", registration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + supervisor.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVALUATOR_ASSIGNMENT_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "A topic supervisor cannot be assigned as that topic's evaluator."));

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", registration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + student.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Evaluator must be an active Lecturer or Faculty Head."));
    }

    @Test
    void onlyApprovedRegistrationsInFacultyHeadDepartmentCanBeAssigned() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("EVAL-OWN-" + suffix);
        DepartmentEntity otherDepartment = department("EVAL-OTHER-" + suffix);
        UserEntity facultyHead = account("eval-scope-head-" + suffix, "Scope Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity evaluator = account("eval-scope-evaluator-" + suffix, "Scope Evaluator " + suffix,
                "LECTURER", ownDepartment);
        TopicRegistrationEntity pending = registration(
                openPeriod(suffix), ownDepartment, "Pending registration " + suffix, TopicRegistrationStatus.PENDING);
        TopicRegistrationEntity otherDepartmentRegistration = registration(
                openPeriod(suffix + "B"), otherDepartment, "Other department registration " + suffix,
                TopicRegistrationStatus.APPROVED);

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", pending.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + evaluator.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only approved topic registrations can have an evaluator assigned."));

        mockMvc.perform(put("/api/faculty/registrations/{id}/evaluator", otherDepartmentRegistration.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluatorId\":" + evaluator.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EVALUATOR_ASSIGNMENT_FORBIDDEN"));
    }

    @Test
    void evaluatorQueueIsDepartmentScopedAndUnauthorizedUsersAreRejected() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("EVAL-QUEUE-" + suffix);
        DepartmentEntity otherDepartment = department("EVAL-QUEUE-OTHER-" + suffix);
        UserEntity facultyHead = account("eval-queue-head-" + suffix, "Queue Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity student = account("eval-queue-student-" + suffix, "Queue Student " + suffix,
                "STUDENT", null);
        registration(
                openPeriod(suffix), ownDepartment, "Visible approved " + suffix, TopicRegistrationStatus.APPROVED);
        registration(
                openPeriod(suffix + "B"), otherDepartment, "Hidden approved " + suffix, TopicRegistrationStatus.APPROVED);
        registration(openPeriod(suffix + "C"), ownDepartment, "Hidden pending " + suffix, TopicRegistrationStatus.PENDING);

        mockMvc.perform(get("/api/faculty/registrations/evaluators")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrations[*].topicTitle").value(hasItem("Visible approved " + suffix)))
                .andExpect(jsonPath("$.registrations[*].topicTitle").value(
                        not(hasItem("Hidden approved " + suffix))))
                .andExpect(jsonPath("$.registrations[*].topicTitle").value(
                        not(hasItem("Hidden pending " + suffix))));

        mockMvc.perform(get("/faculty/registrations/evaluators")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/evaluator-assignments"))
                .andExpect(content().string(containsString("Evaluator assignments")))
                .andExpect(content().string(containsString("Visible approved " + suffix)));

        mockMvc.perform(get("/api/faculty/registrations/evaluators")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isForbidden());
    }

    private TopicRegistrationEntity registration(
            RegistrationPeriodEntity period, DepartmentEntity department, String title,
            TopicRegistrationStatus status) {
        UserEntity proposer = account("eval-proposer-" + suffix(), "Evaluator Proposer", "LECTURER", department);
        UserEntity leader = account("eval-leader-" + suffix(), "Evaluator Leader", "STUDENT", null);
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Evaluator Group " + suffix(), period, leader, leader));
        TopicEntity topic = new TopicEntity(period, department, proposer, title, "Evaluator assignment topic");
        topic.setStatus(TopicStatus.PUBLISHED);
        topic = topicRepository.saveAndFlush(topic);
        TopicRegistrationEntity registration = new TopicRegistrationEntity(group, topic, period, leader);
        registration.setStatus(status);
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

    private RegistrationPeriodEntity openPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Evaluator Period " + suffix, PeriodType.COURSE,
                now.minusHours(1), now.plusHours(1), now.minusHours(1), now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "REGISTRATION_REVIEW");
    }

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return principal(email, "ROLE_STUDENT");
    }

    private static DatabaseUserPrincipal principal(String email, String role, String... permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new DatabaseUserPrincipal(email, "", "Test user", role, authorities);
    }

    private DepartmentEntity department(String code) {
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Evaluator Department " + code));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
