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

import java.math.BigDecimal;
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
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
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
class EvaluationScoringControllerTest {

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

    @Autowired
    private RegistrationResultRepository registrationResultRepository;

    @Test
    void assignedEvaluatorCanSubmitUpdateAndAverageWithoutDuplicateRows() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SCORE-" + suffix);
        UserEntity evaluator = account("score-evaluator-" + suffix, "Score Evaluator " + suffix,
                "LECTURER", department);
        UserEntity secondEvaluator = account("score-second-" + suffix, "Second Evaluator " + suffix,
                "LECTURER", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Scored topic " + suffix, TopicRegistrationStatus.APPROVED);
        EvaluationEntity evaluation = evaluation(registration, evaluator);
        EvaluationEntity secondEvaluation = evaluation(registration, secondEvaluator);
        secondEvaluation.setScore(new BigDecimal("6"));
        secondEvaluation.setStatus(EvaluationStatus.SUBMITTED);
        secondEvaluation.setSubmittedAt(LocalDateTime.now().minusMinutes(5));
        evaluationRepository.saveAndFlush(secondEvaluation);

        mockMvc.perform(put("/api/lecturer/scoring/{id}", evaluation.getId())
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8.50,\"comment\":\"Strong proposal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(8.5))
                .andExpect(jsonPath("$.comment").value("Strong proposal"))
                .andExpect(jsonPath("$.averageScore").value(7.25));

        mockMvc.perform(put("/api/lecturer/scoring/{id}", evaluation.getId())
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":9.00,\"comment\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(7.5));

        Assertions.assertThat(evaluationRepository.findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId()))
                .hasSize(2);
    }

    @Test
    void onlyAssignedEvaluatorCanSubmitAndScoreRangeIsEnforced() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SCORE-RULES-" + suffix);
        UserEntity assignedEvaluator = account("score-assigned-" + suffix, "Assigned Evaluator " + suffix,
                "LECTURER", department);
        UserEntity otherEvaluator = account("score-other-" + suffix, "Other Evaluator " + suffix,
                "LECTURER", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Score rules " + suffix, TopicRegistrationStatus.APPROVED);
        EvaluationEntity evaluation = evaluation(registration, assignedEvaluator);

        mockMvc.perform(put("/api/lecturer/scoring/{id}", evaluation.getId())
                        .with(user(evaluatorPrincipal(otherEvaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8,\"comment\":\"Not assigned\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EVALUATION_FORBIDDEN"));

        mockMvc.perform(put("/api/lecturer/scoring/{id}", evaluation.getId())
                        .with(user(evaluatorPrincipal(assignedEvaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":10.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVALUATION_INVALID"))
                .andExpect(jsonPath("$.message").value("Score must be between 0 and 10."));
    }

    @Test
    void deadlineAndPublishedResultBlockScoreChanges() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SCORE-LOCKS-" + suffix);
        UserEntity evaluator = account("score-lock-evaluator-" + suffix, "Lock Evaluator " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity expiredPeriod = openPeriod(suffix);
        expiredPeriod.setReviewerScoreDeadline(LocalDateTime.now().minusMinutes(1));
        registrationPeriodRepository.saveAndFlush(expiredPeriod);
        TopicRegistrationEntity expiredRegistration = registration(
                expiredPeriod, department, "Expired score " + suffix, TopicRegistrationStatus.APPROVED);
        EvaluationEntity expiredEvaluation = evaluation(expiredRegistration, evaluator);

        mockMvc.perform(put("/api/lecturer/scoring/{id}", expiredEvaluation.getId())
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Evaluation cannot be changed after the reviewer score deadline."));

        TopicRegistrationEntity publishedRegistration = registration(
                openPeriod(suffix + "-published"), department, "Published score " + suffix,
                TopicRegistrationStatus.APPROVED);
        EvaluationEntity publishedEvaluation = evaluation(publishedRegistration, evaluator);
        RegistrationResultEntity result = new RegistrationResultEntity(publishedRegistration);
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        registrationResultRepository.saveAndFlush(result);

        mockMvc.perform(put("/api/lecturer/scoring/{id}", publishedEvaluation.getId())
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Evaluation cannot be changed after the result is published."));
    }

    @Test
    void scoringQueueIsAssignedOnlyAndRendersSsrPage() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SCORE-QUEUE-" + suffix);
        UserEntity evaluator = account("score-queue-evaluator-" + suffix, "Queue Evaluator " + suffix,
                "LECTURER", department);
        UserEntity otherEvaluator = account("score-queue-other-" + suffix, "Queue Other " + suffix,
                "LECTURER", department);
        UserEntity student = account("score-queue-student-" + suffix, "Queue Student " + suffix,
                "STUDENT", null);
        TopicRegistrationEntity visible = registration(
                openPeriod(suffix), department, "Visible evaluation " + suffix, TopicRegistrationStatus.APPROVED);
        TopicRegistrationEntity hidden = registration(
                openPeriod(suffix + "-hidden"), department, "Hidden evaluation " + suffix, TopicRegistrationStatus.APPROVED);
        EvaluationEntity visibleEvaluation = evaluation(visible, evaluator);
        evaluation(hidden, otherEvaluator);

        mockMvc.perform(get("/api/lecturer/scoring")
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluations[*].topicTitle").value(
                        hasItem("Visible evaluation " + suffix)))
                .andExpect(jsonPath("$.evaluations[*].topicTitle").value(
                        not(hasItem("Hidden evaluation " + suffix))));

        mockMvc.perform(get("/lecturer/scoring")
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/scoring"))
                .andExpect(content().string(containsString("Visible evaluation " + suffix)))
                .andExpect(content().string(containsString("My evaluations")));

        mockMvc.perform(put("/api/lecturer/scoring/{id}", visibleEvaluation.getId())
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void scoringQueueSupportsSearchSortAndPagination() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SCORE-FILTER-" + suffix);
        UserEntity evaluator = account("score-filter-evaluator-" + suffix,
                "Filter Evaluator " + suffix, "LECTURER", department);
        for (int index = 1; index <= 6; index++) {
            TopicRegistrationEntity registration = registration(
                    openPeriod(suffix + "-" + index), department,
                    "Sorted evaluation " + String.format(Locale.ROOT, "%02d", index),
                    TopicRegistrationStatus.APPROVED);
            evaluation(registration, evaluator);
        }

        String pageHtml = mockMvc.perform(get("/lecturer/scoring")
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "topic")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Search evaluations")))
                .andExpect(content().string(containsString("Sorted evaluation 06")))
                .andExpect(content().string(not(containsString("Sorted evaluation 01"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Assertions.assertThat(pageHtml).contains("Showing");

        mockMvc.perform(get("/lecturer/scoring")
                        .with(user(evaluatorPrincipal(evaluator.getEmailOrCode())))
                        .param("search", "Sorted evaluation 06")
                        .param("sort", "topic")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sorted evaluation 06")))
                .andExpect(content().string(not(containsString("Sorted evaluation 01"))))
                .andExpect(content().string(containsString("value=\"sorted evaluation 06\"")));
    }

    private EvaluationEntity evaluation(TopicRegistrationEntity registration, UserEntity evaluator) {
        return evaluationRepository.saveAndFlush(new EvaluationEntity(registration, evaluator));
    }

    private TopicRegistrationEntity registration(
            RegistrationPeriodEntity period, DepartmentEntity department, String title,
            TopicRegistrationStatus status) {
        UserEntity proposer = account("score-proposer-" + suffix(), "Score Proposer", "LECTURER", department);
        UserEntity leader = account("score-leader-" + suffix(), "Score Leader", "STUDENT", null);
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Score Group " + suffix(), period, leader, leader));
        TopicEntity topic = new TopicEntity(period, department, proposer, title, "Scoring topic");
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
                "Scoring Period " + suffix, PeriodType.COURSE,
                now.minusHours(1), now.plusHours(1), now.minusHours(1), now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal evaluatorPrincipal(String email) {
        return principal(email, "ROLE_LECTURER", "EVALUATION_SUBMIT");
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
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Scoring Department " + code));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
