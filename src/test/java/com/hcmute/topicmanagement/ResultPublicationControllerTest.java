package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.PermissionEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.RolePermissionEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.ReviewBoardMemberRole;
import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:resultpublicationtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResultPublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

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

    @Autowired
    private ReviewBoardRepository reviewBoardRepository;

    @Autowired
    private ReviewBoardMemberRepository reviewBoardMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void facultyHeadPublishesCompleteResultAndStudentSeesOnlyOwnPublishedGroupResult() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-" + suffix);
        UserEntity facultyHead = account("result-head-" + suffix, "Result Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity evaluator = account("result-evaluator-" + suffix, "Result Evaluator " + suffix,
                "LECTURER", department);
        UserEntity ownStudent = account("result-own-student-" + suffix, "Own Student " + suffix,
                "STUDENT", null);
        UserEntity otherStudent = account("result-other-student-" + suffix, "Other Student " + suffix,
                "STUDENT", null);
        TopicRegistrationEntity ownRegistration = registration(
                openPeriod(suffix), department, "Own published result " + suffix,
                TopicRegistrationStatus.APPROVED, ownStudent);
        EvaluationEntity ownEvaluation = submittedEvaluation(ownRegistration, evaluator, "8.00");
        TopicRegistrationEntity otherRegistration = registration(
                openPeriod(suffix + "-other"), department, "Other published result " + suffix,
                TopicRegistrationStatus.APPROVED, otherStudent);
        submittedEvaluation(otherRegistration, evaluator, "9.00");
        publishDirect(otherRegistration, facultyHead, "9.00");

        mockMvc.perform(post("/api/faculty/results/{id}/publish", ownRegistration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(ownRegistration.getId()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.averageScore").value(8.0));

        RegistrationResultEntity published = registrationResultRepository
                .findById(ownRegistration.getId()).orElseThrow();
        Assertions.assertThat(published.getStatus()).isEqualTo(RegistrationResultStatus.PUBLISHED);
        Assertions.assertThat(ownEvaluation.getId()).isNotNull();

        mockMvc.perform(get("/api/student/results")
                        .with(user(studentPrincipal(ownStudent.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].topicTitle").value(
                        hasItem("Own published result " + suffix)))
                .andExpect(jsonPath("$[*].topicTitle").value(
                        not(hasItem("Other published result " + suffix))));

        mockMvc.perform(get("/student/results")
                        .with(user(studentPrincipal(ownStudent.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("student/results"))
                .andExpect(content().string(containsString("Own published result " + suffix)))
                .andExpect(content().string(not(containsString("Other published result " + suffix))));
    }

    @Test
    void publicationRequiresAtLeastOneAssignedAndSubmittedScoredEvaluation() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-DATA-" + suffix);
        UserEntity facultyHead = account("result-data-head-" + suffix, "Data Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity evaluator = account("result-data-evaluator-" + suffix, "Data Evaluator " + suffix,
                "LECTURER", department);
        TopicRegistrationEntity noEvaluator = registration(
                openPeriod(suffix), department, "Missing evaluator " + suffix,
                TopicRegistrationStatus.APPROVED, account("result-data-student-" + suffix,
                        "Data Student " + suffix, "STUDENT", null));
        TopicRegistrationEntity draft = registration(
                openPeriod(suffix + "-draft"), department, "Draft evaluation " + suffix,
                TopicRegistrationStatus.APPROVED, account("result-draft-student-" + suffix,
                        "Draft Student " + suffix, "STUDENT", null));
        evaluationRepository.saveAndFlush(new EvaluationEntity(draft, evaluator));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", noEvaluator.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESULT_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "A result cannot be published until an evaluator has been assigned."));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", draft.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A result can be published only after every assigned evaluator submits a score."));
    }

    @Test
    void publishedResultIsImmutableAndEvaluatorDeactivationDoesNotEraseHistory() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-LOCK-" + suffix);
        UserEntity facultyHead = account("result-lock-head-" + suffix, "Lock Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity evaluator = account("result-lock-evaluator-" + suffix, "Lock Evaluator " + suffix,
                "LECTURER", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Immutable result " + suffix,
                TopicRegistrationStatus.APPROVED, account("result-lock-student-" + suffix,
                        "Lock Student " + suffix, "STUDENT", null));
        submittedEvaluation(registration, evaluator, "7.50");
        evaluator.setActive(false);
        userRepository.saveAndFlush(evaluator);

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Published results cannot be changed without an audited action."));
    }

    @Test
    void facultyScopeAndRolesAreEnforcedForPublication() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("RESULT-SCOPE-" + suffix);
        DepartmentEntity otherDepartment = department("RESULT-SCOPE-OTHER-" + suffix);
        UserEntity facultyHead = account("result-scope-head-" + suffix, "Scope Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity otherFacultyHead = account("result-scope-other-head-" + suffix, "Other Scope Head " + suffix,
                "FACULTY_HEAD", otherDepartment);
        UserEntity evaluator = account("result-scope-evaluator-" + suffix, "Scope Evaluator " + suffix,
                "LECTURER", otherDepartment);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), otherDepartment, "Cross department result " + suffix,
                TopicRegistrationStatus.APPROVED, account("result-scope-student-" + suffix,
                        "Scope Student " + suffix, "STUDENT", null));
        submittedEvaluation(registration, evaluator, "8.00");

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESULT_FORBIDDEN"));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(otherFacultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void facultyQueueShowsPublicationStateAndStudentCannotAccessIt() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-QUEUE-" + suffix);
        UserEntity facultyHead = account("result-queue-head-" + suffix, "Queue Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity student = account("result-queue-student-" + suffix, "Queue Student " + suffix,
                "STUDENT", null);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Queue result " + suffix,
                TopicRegistrationStatus.APPROVED, student);
        submittedEvaluation(registration, account("result-queue-evaluator-" + suffix,
                "Queue Evaluator " + suffix, "LECTURER", department), "8.25");

        mockMvc.perform(get("/api/faculty/results")
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].topicTitle").value(hasItem("Queue result " + suffix)))
                .andExpect(jsonPath("$.results[0].publishable").value(true));

        mockMvc.perform(get("/faculty/results")
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/results"))
                .andExpect(content().string(containsString("Queue result " + suffix)));

        mockMvc.perform(get("/api/faculty/results")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void boardResultCannotBePublishedBeforeBoardIsCompleted() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-BOARD-STATUS-" + suffix);
        UserEntity facultyHead = account("result-board-status-head-" + suffix, "Board Status Head " + suffix,
                "FACULTY_HEAD", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Board status result " + suffix,
                TopicRegistrationStatus.APPROVED,
                account("result-board-status-student-" + suffix, "Board Status Student " + suffix,
                        "STUDENT", null));
        List<UserEntity> evaluators = boardEvaluators(suffix, department);
        ReviewBoardEntity board = boardWithScores(registration, facultyHead, evaluators,
                ReviewBoardStatus.ACTIVE, List.of("8.00", "9.00", "8.50"));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A review board must be completed before its result can be published."));

        Assertions.assertThat(reviewBoardRepository.findById(board.getId()).orElseThrow().getStatus())
                .isEqualTo(ReviewBoardStatus.ACTIVE);
    }

    @Test
    void completedBoardPublishesRoundedAverageAndLocksBoard() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-BOARD-PUBLISH-" + suffix);
        UserEntity facultyHead = account("result-board-publish-head-" + suffix, "Board Publish Head " + suffix,
                "FACULTY_HEAD", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Completed board result " + suffix,
                TopicRegistrationStatus.APPROVED,
                account("result-board-publish-student-" + suffix, "Board Publish Student " + suffix,
                        "STUDENT", null));
        List<UserEntity> evaluators = boardEvaluators(suffix, department);
        ReviewBoardEntity board = boardWithScores(registration, facultyHead, evaluators,
                ReviewBoardStatus.COMPLETED, List.of("8.00", "8.01", "8.02"));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.averageScore").value(8.01));

        Assertions.assertThat(reviewBoardRepository.findById(board.getId()).orElseThrow().getStatus())
                .isEqualTo(ReviewBoardStatus.PUBLISHED);
        Assertions.assertThat(registrationResultRepository.findById(registration.getId()).orElseThrow().getAverageScore())
                .isEqualByComparingTo("8.01");
    }

    @Test
    void inactiveHistoricalBoardMemberDoesNotBlockCurrentAverage() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-BOARD-HISTORY-" + suffix);
        UserEntity facultyHead = account("result-board-history-head-" + suffix, "Board History Head " + suffix,
                "FACULTY_HEAD", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Historical board result " + suffix,
                TopicRegistrationStatus.APPROVED,
                account("result-board-history-student-" + suffix, "Board History Student " + suffix,
                        "STUDENT", null));
        List<UserEntity> evaluators = boardEvaluators(suffix, department);
        ReviewBoardEntity board = boardWithScores(registration, facultyHead, evaluators,
                ReviewBoardStatus.COMPLETED, List.of("8.00", "9.00", "10.00"));
        ReviewBoardMemberEntity historicalMember = reviewBoardMemberRepository
                .findByBoard_IdAndLecturer_Id(board.getId(), evaluators.get(2).getId()).orElseThrow();
        historicalMember.setActive(false);
        historicalMember.setEndedAt(LocalDateTime.now().minusMinutes(1));
        reviewBoardMemberRepository.saveAndFlush(historicalMember);

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(8.5))
                .andExpect(jsonPath("$.evaluationCount").value(2))
                .andExpect(jsonPath("$.submittedEvaluationCount").value(2));
    }

    @Test
    void invalidBoardScoreCannotSatisfyPublication() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-BOARD-INVALID-" + suffix);
        UserEntity facultyHead = account("result-board-invalid-head-" + suffix, "Board Invalid Head " + suffix,
                "FACULTY_HEAD", department);
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Invalid board result " + suffix,
                TopicRegistrationStatus.APPROVED,
                account("result-board-invalid-student-" + suffix, "Board Invalid Student " + suffix,
                        "STUDENT", null));
        List<UserEntity> evaluators = boardEvaluators(suffix, department);
        boardWithScores(registration, facultyHead, evaluators,
                ReviewBoardStatus.COMPLETED, List.of("8.00", "11.00", "8.50"));

        mockMvc.perform(post("/api/faculty/results/{id}/publish", registration.getId())
                        .with(user(facultyPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A result can be published only after every assigned evaluator submits a score."));
    }

    @Test
    void studentCanLoginAndReadOnlyOwnPublishedResultEndToEnd() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("RESULT-LOGIN-" + suffix);
        UserEntity student = account("result-login-student-" + suffix, "Login Student " + suffix,
                "STUDENT", null);
        student.setPasswordHash(passwordEncoder.encode("student-password"));
        userRepository.saveAndFlush(student);
        RoleEntity studentRole = roleRepository.findByCode("STUDENT").orElseThrow();
        PermissionEntity resultView = permissionRepository.findByCode("RESULT_VIEW")
                .orElseGet(() -> permissionRepository.saveAndFlush(
                        new PermissionEntity("RESULT_VIEW", "View results", "Results")));
        rolePermissionRepository.saveAndFlush(new RolePermissionEntity(studentRole, resultView));
        TopicRegistrationEntity registration = registration(
                openPeriod(suffix), department, "Login published result " + suffix,
                TopicRegistrationStatus.APPROVED, student);
        UserEntity evaluator = account("result-login-evaluator-" + suffix, "Login Evaluator " + suffix,
                "LECTURER", department);
        submittedEvaluation(registration, evaluator, "8.75");
        UserEntity publisher = account("result-login-head-" + suffix, "Login Head " + suffix,
                "FACULTY_HEAD", department);
        publishDirect(registration, publisher, "8.75");

        var login = mockMvc.perform(post("/login")
                        .param("email", student.getEmailOrCode())
                        .param("password", "student-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/student/results").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student/results"))
                .andExpect(content().string(containsString("Login published result " + suffix)));
    }

    private EvaluationEntity submittedEvaluation(
            TopicRegistrationEntity registration, UserEntity evaluator, String score) {
        EvaluationEntity evaluation = new EvaluationEntity(registration, evaluator);
        evaluation.setScore(new BigDecimal(score));
        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(LocalDateTime.now().minusMinutes(2));
        return evaluationRepository.saveAndFlush(evaluation);
    }

    private void publishDirect(TopicRegistrationEntity registration, UserEntity publisher, String average) {
        RegistrationResultEntity result = new RegistrationResultEntity(registration);
        result.setAverageScore(new BigDecimal(average));
        result.setStatus(RegistrationResultStatus.PUBLISHED);
        result.setPublishedBy(publisher);
        result.setPublishedAt(LocalDateTime.now().minusMinutes(1));
        registrationResultRepository.saveAndFlush(result);
    }

    private List<UserEntity> boardEvaluators(String suffix, DepartmentEntity department) {
        return List.of(
                account("result-board-evaluator-1-" + suffix, "Board Evaluator 1 " + suffix, "LECTURER", department),
                account("result-board-evaluator-2-" + suffix, "Board Evaluator 2 " + suffix, "LECTURER", department),
                account("result-board-evaluator-3-" + suffix, "Board Evaluator 3 " + suffix, "LECTURER", department));
    }

    private ReviewBoardEntity boardWithScores(
            TopicRegistrationEntity registration,
            UserEntity creator,
            List<UserEntity> evaluators,
            ReviewBoardStatus status,
            List<String> scores) {
        ReviewBoardEntity board = new ReviewBoardEntity(registration, creator);
        board.setStatus(status);
        board = reviewBoardRepository.saveAndFlush(board);
        ReviewBoardMemberRole[] roles = {
                ReviewBoardMemberRole.CHAIR,
                ReviewBoardMemberRole.SECRETARY,
                ReviewBoardMemberRole.MEMBER};
        for (int index = 0; index < evaluators.size(); index++) {
            ReviewBoardMemberEntity member = new ReviewBoardMemberEntity(board, evaluators.get(index));
            member.setMemberRole(roles[index]);
            member = reviewBoardMemberRepository.saveAndFlush(member);
            EvaluationEntity evaluation = new EvaluationEntity(registration, evaluators.get(index));
            evaluation.setBoard(board);
            evaluation.setBoardMember(member);
            evaluation.setScore(new BigDecimal(scores.get(index)));
            evaluation.setStatus(EvaluationStatus.SUBMITTED);
            evaluation.setSubmittedAt(LocalDateTime.now().minusMinutes(1));
            evaluationRepository.saveAndFlush(evaluation);
        }
        return board;
    }

    private TopicRegistrationEntity registration(
            RegistrationPeriodEntity period, DepartmentEntity department, String title,
            TopicRegistrationStatus status, UserEntity leader) {
        UserEntity proposer = account("result-proposer-" + suffix(), "Result Proposer", "LECTURER", department);
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Result Group " + suffix(), period, leader, leader));
        TopicEntity topic = new TopicEntity(period, department, proposer, title, "Result publication topic");
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
                "Result Period " + suffix, PeriodType.COURSE,
                now.minusHours(1), now.plusHours(1), now.minusHours(1), now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal facultyPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "REGISTRATION_REVIEW");
    }

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return principal(email, "ROLE_STUDENT", "RESULT_VIEW");
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
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Result Department " + code));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
