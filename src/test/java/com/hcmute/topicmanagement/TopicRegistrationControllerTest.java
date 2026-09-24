package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
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
class TopicRegistrationControllerTest {

    private DepartmentEntity testStudentDepartment;

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
    void leaderCanSubmitAndStudentCanViewRegistration() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-leader-" + suffix, "Registration Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("Registration Group " + suffix, period, leader);
        TopicEntity topic = saveTopic(period, "Published Topic " + suffix, TopicStatus.PUBLISHED);

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), topic.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(group.getId()))
                .andExpect(jsonPath("$.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.statusCode").value("PENDING"))
                .andExpect(jsonPath("$.submittedById").value(leader.getId()))
                .andExpect(jsonPath("$.submittedAt").exists());

        mockMvc.perform(get("/api/student/registrations")
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].topicTitle").value(hasItem(topic.getTitle())))
                .andExpect(jsonPath("$[*].groupName").value(hasItem(group.getName())));

        mockMvc.perform(get("/student/registrations")
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("student/registrations"))
                .andExpect(content().string(containsString(topic.getTitle())))
                .andExpect(content().string(containsString("Pending")))
                .andExpect(content().string(containsString("Filter registrations by status")))
                .andExpect(content().string(containsString("Search registrations")));

        mockMvc.perform(get("/student/registrations")
                        .param("status", "APPROVED")
                        .param("search", "missing")
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No matching registrations")))
                .andExpect(content().string(containsString("Filter registrations by status")))
                .andExpect(content().string(containsString("value=\"APPROVED\"")));
    }

    @Test
    void registrationFormListsPublishedTopicsForTheGroupPeriod() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-form-" + suffix, "Registration Form " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("Form Group " + suffix, period, leader);
        TopicEntity topic = saveTopic(period, "Form Topic " + suffix, TopicStatus.PUBLISHED);

        mockMvc.perform(get("/student/groups/register-topic")
                        .param("groupId", group.getId().toString())
                        .with(user(studentPrincipal(leader.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("student/topic-registration"))
                .andExpect(content().string(containsString(topic.getTitle())))
                .andExpect(content().string(containsString(group.getName())));

        mockMvc.perform(post("/student/groups/register-topic")
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .param("groupId", group.getId().toString())
                        .param("periodId", period.getId().toString())
                        .param("topicId", topic.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/student/registrations"));
        org.assertj.core.api.Assertions.assertThat(
                topicRegistrationRepository.findByStudentGroup_IdOrderBySubmittedAtDesc(group.getId()))
                .hasSize(1);
    }

    @Test
    void onlyCurrentLeaderCanSubmitRegistration() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-owner-" + suffix, "Registration Owner " + suffix);
        UserEntity member = student("registration-member-" + suffix, "Registration Member " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("Leader Only Group " + suffix, period, leader);
        group.getMembers().add(member);
        studentGroupRepository.saveAndFlush(group);
        TopicEntity topic = saveTopic(period, "Leader Only Topic " + suffix, TopicStatus.PUBLISHED);

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(member.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), topic.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOPIC_REGISTRATION_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(
                        "Only the current group leader can submit a topic registration."));
    }

    @Test
    void unpublishedTopicAndInactiveGroupAreRejected() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-state-" + suffix, "Registration State " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("State Group " + suffix, period, leader);
        TopicEntity approved = saveTopic(period, "Approved Only Topic " + suffix, TopicStatus.APPROVED);

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), approved.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only published topics can be registered."));

        TopicEntity published = saveTopic(period, "Inactive Group Topic " + suffix, TopicStatus.PUBLISHED);
        group.setStatus(GroupStatus.COMPLETED);
        studentGroupRepository.saveAndFlush(group);
        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), published.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only active groups can submit topic registrations."));
    }

    @Test
    void topicMustBelongToSelectedGroupPeriod() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-period-" + suffix, "Registration Period " + suffix);
        RegistrationPeriodEntity groupPeriod = openPeriod(suffix + "-GROUP", -2, 2);
        RegistrationPeriodEntity otherPeriod = openPeriod(suffix + "-OTHER", -2, 2);
        StudentGroupEntity group = saveGroup("Period Group " + suffix, groupPeriod, leader);
        TopicEntity topic = saveTopic(otherPeriod, "Other Period Topic " + suffix, TopicStatus.PUBLISHED);

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(groupPeriod.getId(), topic.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The selected topic must belong to the group's registration period."));
    }

    @Test
    void registrationWindowDeadlineIsEnforced() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-deadline-" + suffix, "Registration Deadline " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -3, -2);
        StudentGroupEntity group = saveGroup("Deadline Group " + suffix, period, leader);
        TopicEntity topic = saveTopic(period, "Deadline Topic " + suffix, TopicStatus.PUBLISHED);

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), topic.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The student registration window is closed."));
    }

    @Test
    void oneGroupCannotHaveMultipleCurrentRegistrationsInOnePeriod() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-duplicate-" + suffix, "Registration Duplicate " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("Duplicate Group " + suffix, period, leader);
        TopicEntity first = saveTopic(period, "First Topic " + suffix, TopicStatus.PUBLISHED);
        TopicEntity second = saveTopic(period, "Second Topic " + suffix, TopicStatus.PUBLISHED);
        String body = registrationJson(period.getId(), first.getId());

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/student/groups/{groupId}/registrations", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(period.getId(), second.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "This group already has a current topic registration in this period."));

        org.assertj.core.api.Assertions.assertThat(
                topicRegistrationRepository.findByStudentGroup_IdOrderBySubmittedAtDesc(group.getId()))
                .hasSize(1)
                .first()
                .extracting(registration -> registration.getStatus())
                .isEqualTo(TopicRegistrationStatus.PENDING);
    }

    @Test
    void studentOnlySeesRegistrationsFromTheirOwnGroups() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("registration-visible-" + suffix, "Registration Visible " + suffix);
        UserEntity unrelated = student("registration-unrelated-" + suffix, "Registration Unrelated " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix, -2, 2);
        StudentGroupEntity group = saveGroup("Visible Group " + suffix, period, leader);
        TopicEntity topic = saveTopic(period, "Visible Topic " + suffix, TopicStatus.PUBLISHED);
        topicRegistrationRepository.saveAndFlush(
                new com.hcmute.topicmanagement.model.TopicRegistrationEntity(group, topic, period, leader));

        mockMvc.perform(get("/api/student/registrations")
                        .with(user(studentPrincipal(unrelated.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    private StudentGroupEntity saveGroup(String name, RegistrationPeriodEntity period, UserEntity leader) {
        return studentGroupRepository.saveAndFlush(new StudentGroupEntity(name, period, leader, leader));
    }

    private TopicEntity saveTopic(RegistrationPeriodEntity period, String title, TopicStatus status) {
        DepartmentEntity department = testStudentDepartment();
        UserEntity lecturer = account("registration-lecturer-" + suffix(), "Registration Lecturer " + suffix(), "LECTURER");
        TopicEntity topic = new TopicEntity(period, department, lecturer, title, "Topic description");
        topic.setStatus(status);
        return topicRepository.saveAndFlush(topic);
    }

    private RegistrationPeriodEntity openPeriod(String suffix, int startHours, int endHours) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Registration Period " + suffix, PeriodType.COURSE,
                now.minusHours(3), now.plusHours(3),
                now.plusHours(startHours), now.plusHours(endHours));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private UserEntity student(String login, String fullName) {
        UserEntity student = account(login, fullName, "STUDENT");
        student.setDepartment(testStudentDepartment());
        return userRepository.saveAndFlush(student);
    }

    private DepartmentEntity testStudentDepartment() {
        if (testStudentDepartment == null) {
            String suffix = suffix();
            testStudentDepartment = departmentRepository.saveAndFlush(
                    new DepartmentEntity("REG-" + suffix, "Registration Department " + suffix));
        }
        return testStudentDepartment;
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

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Test student", "Student",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("TOPIC_VIEW"),
                        new SimpleGrantedAuthority("GROUP_MANAGE"),
                        new SimpleGrantedAuthority("REGISTRATION_SUBMIT")));
    }

    private static String registrationJson(Long periodId, Long topicId) {
        return "{\"periodId\":" + periodId + ",\"topicId\":" + topicId + "}";
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
