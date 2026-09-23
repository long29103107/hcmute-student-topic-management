package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TopicPublicationControllerTest {

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
    private TopicRepository topicRepository;

    @Test
    void facultyHeadCanPublishApprovedTopicAndStudentCanSeeIt() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("PUB-" + suffix);
        UserEntity facultyHead = saveUser("publish-head-" + suffix, "Publish Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity proposer = saveUser("publish-proposer-" + suffix, "Publish Proposer " + suffix,
                "LECTURER", department);
        UserEntity student = saveUser("publish-student-" + suffix, "Publish Student " + suffix,
                "STUDENT", department);
        RegistrationPeriodEntity period = openPeriod(suffix, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));
        TopicEntity topic = topic(period, department, proposer, "visible-" + suffix, TopicStatus.APPROVED);

        mockMvc.perform(get("/faculty/topics/publish")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/publish"))
                .andExpect(content().string(containsString(topic.getTitle())))
                .andExpect(content().string(containsString("sort=topic")))
                .andExpect(content().string(containsString("sort=period")))
                .andExpect(content().string(containsString("sort=proposer")));

        mockMvc.perform(post("/api/faculty/topics/{id}/publish", topic.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value(topic.getTitle()));

        mockMvc.perform(get("/api/topics")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem(topic.getTitle())))
                .andExpect(jsonPath("$[*].statusCode").value(hasItem("PUBLISHED")));

        mockMvc.perform(get("/topics")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("topics"))
                .andExpect(content().string(containsString(topic.getTitle())));

        org.assertj.core.api.Assertions.assertThat(topicRepository.findById(topic.getId()).orElseThrow().getStatus())
                .isEqualTo(TopicStatus.PUBLISHED);
    }

    @Test
    void publishedQueryRequiresPublishedStatusAndValidStudentPeriod() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("VIS-" + suffix);
        UserEntity student = saveUser("visibility-student-" + suffix, "Visibility Student " + suffix,
                "STUDENT", department);
        UserEntity proposer = saveUser("visibility-proposer-" + suffix, "Visibility Proposer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity validPeriod = openPeriod(suffix + "-VALID",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        RegistrationPeriodEntity futurePeriod = openPeriod(suffix + "-FUTURE",
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        RegistrationPeriodEntity closedPeriod = openPeriod(suffix + "-CLOSED",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        closedPeriod.setStatus(RegistrationPeriodStatus.CLOSED);
        registrationPeriodRepository.saveAndFlush(closedPeriod);

        TopicEntity visible = topic(validPeriod, department, proposer, "visible-query-" + suffix,
                TopicStatus.PUBLISHED);
        TopicEntity approved = topic(validPeriod, department, proposer, "approved-query-" + suffix,
                TopicStatus.APPROVED);
        TopicEntity draft = topic(validPeriod, department, proposer, "draft-query-" + suffix,
                TopicStatus.DRAFT);
        TopicEntity rejected = topic(validPeriod, department, proposer, "rejected-query-" + suffix,
                TopicStatus.REJECTED);
        TopicEntity future = topic(futurePeriod, department, proposer, "future-query-" + suffix,
                TopicStatus.PUBLISHED);
        TopicEntity closed = topic(closedPeriod, department, proposer, "closed-query-" + suffix,
                TopicStatus.PUBLISHED);

        mockMvc.perform(get("/api/topics")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem(visible.getTitle())))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(approved.getTitle()))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(draft.getTitle()))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(rejected.getTitle()))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(future.getTitle()))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(closed.getTitle()))));

        mockMvc.perform(get("/api/topics").param("periodId", validPeriod.getId().toString())
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem(visible.getTitle())))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(future.getTitle()))));
    }

    @Test
    void onlyApprovedTopicsCanBePublishedAndPublishedProposalCannotBeEdited() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("STATE-" + suffix);
        UserEntity facultyHead = saveUser("state-head-" + suffix, "State Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity proposer = saveUser("state-proposer-" + suffix, "State Proposer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        TopicEntity draft = topic(period, department, proposer, "draft-state-" + suffix, TopicStatus.DRAFT);
        TopicEntity approved = topic(period, department, proposer, "approved-state-" + suffix,
                TopicStatus.APPROVED);

        mockMvc.perform(post("/api/topics/{id}/publish", draft.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_PUBLICATION_INVALID"));

        mockMvc.perform(post("/api/topics/{id}/publish", approved.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/topics/{id}/publish", approved.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only approved topic proposals can be published."));

        mockMvc.perform(put("/api/lecturer/topics/{id}", approved.getId())
                        .with(user(lecturerPrincipal(proposer.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"changed\",\"description\":\"changed description\","
                                + "\"departmentId\":" + department.getId() + ",\"periodId\":"
                                + period.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only draft or rejected topic proposals can be edited."));
    }

    @Test
    void publishIsScopedToFacultyHeadDepartmentWhileAdminCanPublishAcrossDepartments() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("OWN-" + suffix);
        DepartmentEntity otherDepartment = department("OTHER-" + suffix);
        UserEntity facultyHead = saveUser("scope-head-" + suffix, "Scope Head " + suffix,
                "FACULTY_HEAD", ownDepartment);
        UserEntity otherFacultyHead = saveUser("other-head-" + suffix, "Other Head " + suffix,
                "FACULTY_HEAD", otherDepartment);
        UserEntity admin = saveUser("publish-admin-" + suffix, "Publish Admin " + suffix, "ADMIN", null);
        UserEntity lecturer = saveUser("publish-lecturer-" + suffix, "Publish Lecturer " + suffix,
                "LECTURER", ownDepartment);
        UserEntity proposer = saveUser("scope-proposer-" + suffix, "Scope Proposer " + suffix,
                "LECTURER", otherDepartment);
        RegistrationPeriodEntity period = openPeriod(suffix,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        TopicEntity topic = topic(period, otherDepartment, proposer, "other-department-" + suffix,
                TopicStatus.APPROVED);

        mockMvc.perform(get("/faculty/topics/publish")
                        .with(user(adminPrincipal(admin.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(topic.getTitle())));

        mockMvc.perform(post("/api/topics/{id}/publish", topic.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOPIC_PUBLICATION_FORBIDDEN"));

        mockMvc.perform(post("/api/topics/{id}/publish", topic.getId())
                        .with(user(facultyHeadPrincipal(otherFacultyHead.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk());

        TopicEntity adminTopic = topic(period, ownDepartment, proposer, "admin-topic-" + suffix,
                TopicStatus.APPROVED);
        mockMvc.perform(post("/api/topics/{id}/publish", adminTopic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("PUBLISHED"));

        mockMvc.perform(post("/api/topics/{id}/publish", adminTopic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishMutationRequiresCsrfAndPublishedCatalogRequiresPermission() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SEC-" + suffix);
        UserEntity facultyHead = saveUser("csrf-publish-head-" + suffix, "CSRF Publish Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity student = saveUser("csrf-publish-student-" + suffix, "CSRF Publish Student " + suffix,
                "STUDENT", department);
        UserEntity proposer = saveUser("csrf-publish-proposer-" + suffix, "CSRF Publish Proposer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        TopicEntity topic = topic(period, department, proposer, "csrf-publish-" + suffix,
                TopicStatus.APPROVED);

        mockMvc.perform(post("/api/topics/{id}/publish", topic.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/topics")
                        .with(user(principal(student.getEmailOrCode(), "ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    private TopicEntity topic(
            RegistrationPeriodEntity period, DepartmentEntity department, UserEntity proposer, String name,
            TopicStatus status) {
        TopicEntity topic = new TopicEntity(
                period, department, proposer, "Publication topic " + name, "Publication topic description");
        topic.setStatus(status);
        return topicRepository.saveAndFlush(topic);
    }

    private UserEntity saveUser(String loginPrefix, String fullName, String roleCode, DepartmentEntity department) {
        UserEntity user = new UserEntity(loginPrefix, fullName, "test-password-hash");
        user.setEmailOrCode(loginPrefix.toLowerCase(Locale.ROOT) + "@hcmute.edu.vn");
        user.setDepartment(department);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        userRoleRepository.saveAndFlush(new UserRoleEntity(user, role));
        return user;
    }

    private DepartmentEntity department(String code) {
        return departmentRepository.saveAndFlush(new DepartmentEntity(code, "Publication Department " + code));
    }

    private RegistrationPeriodEntity openPeriod(String suffix, LocalDateTime studentStart,
                                                LocalDateTime studentEnd) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Publication Period " + suffix, PeriodType.COURSE,
                now.minusHours(2), now.plusHours(2), studentStart, studentEnd);
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "TOPIC_REVIEW");
    }

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return principal(email, "ROLE_STUDENT", "TOPIC_VIEW");
    }

    private static DatabaseUserPrincipal lecturerPrincipal(String email) {
        return principal(email, "ROLE_LECTURER", "TOPIC_PROPOSE");
    }

    private static DatabaseUserPrincipal adminPrincipal(String email) {
        return principal(email, "ROLE_ADMIN", "TOPIC_REVIEW");
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
