package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class TopicSupervisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void adminCanViewAndAssignOneOrTwoSupervisors() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("admin-" + suffix, "Admin " + suffix, "ADMIN", null);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("proposer-" + suffix, "Proposer " + suffix, "LECTURER", department);
        UserEntity first = saveUser("first-supervisor-" + suffix, "First Supervisor " + suffix, "LECTURER", department);
        UserEntity second = saveUser("second-supervisor-" + suffix, "Second Supervisor " + suffix, "LECTURER", department);
        TopicEntity topic = topic(period, department, proposer, suffix);

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/supervisors"))
                .andExpect(content().string(containsString(topic.getTitle())))
                .andExpect(content().string(containsString(first.getFullName())))
                .andExpect(content().string(containsString("Admin · all departments")));

        mockMvc.perform(post("/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .param("lecturerIds", first.getId().toString(), second.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/topics/supervisors"))
                .andExpect(flash().attribute("successMessage", "Topic supervisors updated successfully."));

        TopicEntity assigned = topicRepository.findByIdForSupervisorManagement(topic.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(assigned.getSupervisors())
                .extracting(UserEntity::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void facultyHeadOnlySeesAndManagesTopicsInOwnDepartment() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = department("OWN-" + suffix);
        DepartmentEntity otherDepartment = department("OTHER-" + suffix);
        UserEntity facultyHead = saveUser("head-" + suffix, "Faculty Head " + suffix, "FACULTY_HEAD", ownDepartment);
        UserEntity proposer = saveUser("own-proposer-" + suffix, "Own Proposer " + suffix, "LECTURER", ownDepartment);
        UserEntity supervisor = saveUser("own-supervisor-" + suffix, "Own Supervisor " + suffix, "LECTURER", ownDepartment);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity ownTopic = topic(period, ownDepartment, proposer, "own-" + suffix);
        TopicEntity otherTopic = topic(period, otherDepartment, proposer, "other-" + suffix);

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(ownTopic.getTitle())))
                .andExpect(content().string(not(containsString(otherTopic.getTitle()))))
                .andExpect(content().string(containsString("Faculty Head · " + ownDepartment.getCode())));

        mockMvc.perform(post("/faculty/topics/{id}/supervisors", otherTopic.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .param("lecturerIds", supervisor.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forbidden"));

        org.assertj.core.api.Assertions.assertThat(
                topicRepository.findByIdForSupervisorManagement(otherTopic.getId()).orElseThrow().getSupervisors())
                .isEmpty();
    }

    @Test
    void assignmentRejectsEmptyDuplicateOverLimitAndInvalidUsers() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("validation-admin-" + suffix, "Validation Admin " + suffix, "ADMIN", null);
        DepartmentEntity department = department("VALID-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("validation-proposer-" + suffix, "Validation Proposer " + suffix, "LECTURER", department);
        UserEntity first = saveUser("validation-first-" + suffix, "Validation First " + suffix, "LECTURER", department);
        UserEntity second = saveUser("validation-second-" + suffix, "Validation Second " + suffix, "LECTURER", department);
        UserEntity student = saveUser("validation-student-" + suffix, "Validation Student " + suffix, "STUDENT", department);
        UserEntity inactive = saveUser("validation-inactive-" + suffix, "Validation Inactive " + suffix, "LECTURER", department);
        inactive.setActive(false);
        userRepository.saveAndFlush(inactive);
        TopicEntity topic = topic(period, department, proposer, "validation-" + suffix);

        assertBadAssignment(admin, topic, "Select at least one supervisor.");
        assertBadAssignment(admin, topic, "A supervisor cannot be selected more than once.",
                first.getId(), first.getId());
        assertBadAssignment(admin, topic, "A topic can have at most two supervisors.",
                first.getId(), second.getId(), proposer.getId());
        assertBadAssignment(admin, topic,
                "Every selected supervisor must be an active Lecturer or Faculty Head.", student.getId());
        assertBadAssignment(admin, topic,
                "Every selected supervisor must be an active Lecturer or Faculty Head.", inactive.getId());

        org.assertj.core.api.Assertions.assertThat(
                topicRepository.findByIdForSupervisorManagement(topic.getId()).orElseThrow().getSupervisors())
                .isEmpty();
    }

    @Test
    void restUsesTheSameAssignmentRulesAndRequiresPermissionAndCsrf() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("rest-admin-" + suffix, "REST Admin " + suffix, "ADMIN", null);
        DepartmentEntity department = department("REST-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("rest-proposer-" + suffix, "REST Proposer " + suffix, "LECTURER", department);
        UserEntity supervisor = saveUser("rest-supervisor-" + suffix, "REST Supervisor " + suffix, "LECTURER", department);
        TopicEntity topic = topic(period, department, proposer, "rest-" + suffix);

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].title").value(topic.getTitle()))
                .andExpect(jsonPath("$.supervisorOptions").isArray());

        mockMvc.perform(put("/api/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .contentType("application/json")
                        .content("{\"lecturerIds\":[" + supervisor.getId() + "]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"lecturerIds\":[" + supervisor.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topic.getId()))
                .andExpect(jsonPath("$.supervisors[0].id").value(supervisor.getId()));

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorCandidatesIncludeFacultyHeadsAndOnlyActiveLecturerCapabilities() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("option-admin-" + suffix, "Option Admin " + suffix, "ADMIN", null);
        DepartmentEntity department = department("OPTION-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("option-proposer-" + suffix, "Option Proposer " + suffix, "LECTURER", department);
        UserEntity facultyHead = saveUser("option-head-" + suffix, "Option Faculty Head " + suffix, "FACULTY_HEAD", department);
        UserEntity student = saveUser("option-student-" + suffix, "Option Student " + suffix, "STUDENT", department);
        TopicEntity topic = topic(period, department, proposer, "options-" + suffix);

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supervisorOptions[*].id").value(org.hamcrest.Matchers.hasItem(facultyHead.getId().intValue())))
                .andExpect(jsonPath("$.supervisorOptions[*].id").value(not(org.hamcrest.Matchers.hasItem(student.getId().intValue()))));

        mockMvc.perform(put("/api/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"lecturerIds\":[" + facultyHead.getId() + "]}"))
                .andExpect(status().isOk());
    }

    private void assertBadAssignment(UserEntity admin, TopicEntity topic, String message, Long... ids)
            throws Exception {
        var request = post("/faculty/topics/{id}/supervisors", topic.getId())
                .with(user(adminPrincipal(admin.getEmailOrCode())))
                .with(csrf());
        for (Long id : ids) {
            request = request.param("lecturerIds", id.toString());
        }
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/topics/supervisors"))
                .andExpect(flash().attribute("errorMessage", message));
    }

    private TopicEntity topic(RegistrationPeriodEntity period, DepartmentEntity department,
                              UserEntity proposer, String name) {
        return topicRepository.saveAndFlush(new TopicEntity(
                period, department, proposer, "Supervisor topic " + name, "Supervisor topic description"));
    }

    private UserEntity saveUser(String loginPrefix, String fullName, String roleCode, DepartmentEntity department) {
        UserEntity user = new UserEntity(loginPrefix, fullName, "test-password-hash");
        user.setEmailOrCode(loginPrefix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn");
        user.setDepartment(department);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        userRoleRepository.saveAndFlush(new UserRoleEntity(user, role));
        return user;
    }

    private DepartmentEntity department(String suffix) {
        return departmentRepository.saveAndFlush(
                new DepartmentEntity("SUP-" + suffix, "Supervisor Department " + suffix));
    }

    private RegistrationPeriodEntity openPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Supervisor Period " + suffix,
                PeriodType.COURSE,
                now.minusHours(1), now.plusHours(1), now.minusHours(1), now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal adminPrincipal(String email) {
        return principal(email, "ROLE_ADMIN", "SUPERVISOR_MANAGE");
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "SUPERVISOR_MANAGE");
    }

    private static DatabaseUserPrincipal principal(String email, String role, String permission) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        authorities.add(new SimpleGrantedAuthority(permission));
        return new DatabaseUserPrincipal(email, "", "Test user", role, authorities);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
