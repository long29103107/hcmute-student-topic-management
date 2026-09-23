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
                .andExpect(content().string(containsString("Admin · all departments")))
                .andExpect(content().string(not(containsString("No data"))));

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
        DepartmentEntity otherDepartment = department("OTHER-VALID-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("validation-proposer-" + suffix, "Validation Proposer " + suffix, "LECTURER", department);
        UserEntity first = saveUser("validation-first-" + suffix, "Validation First " + suffix, "LECTURER", department);
        UserEntity second = saveUser("validation-second-" + suffix, "Validation Second " + suffix, "LECTURER", department);
        UserEntity student = saveUser("validation-student-" + suffix, "Validation Student " + suffix, "STUDENT", department);
        UserEntity inactive = saveUser("validation-inactive-" + suffix, "Validation Inactive " + suffix, "LECTURER", department);
        UserEntity otherDepartmentSupervisor = saveUser(
                "validation-other-" + suffix, "Validation Other " + suffix, "LECTURER", otherDepartment);
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
        assertBadAssignment(admin, topic,
                "Every selected supervisor must belong to the topic's department.",
                otherDepartmentSupervisor.getId());

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
        DepartmentEntity otherDepartment = department("OTHER-REST-" + suffix);
        UserEntity otherDepartmentSupervisor = saveUser(
                "rest-other-" + suffix, "REST Other " + suffix, "LECTURER", otherDepartment);
        TopicEntity topic = topic(period, department, proposer, "rest-" + suffix);

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("search", "rest-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].title").value(topic.getTitle()))
                .andExpect(jsonPath("$.topics[0].supervisorOptions[*].id")
                        .value(org.hamcrest.Matchers.hasItem(supervisor.getId().intValue())));

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

        mockMvc.perform(put("/api/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"lecturerIds\":[" + otherDepartmentSupervisor.getId() + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_SUPERVISOR_INVALID"));

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
        DepartmentEntity otherDepartment = department("OTHER-OPTION-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity proposer = saveUser("option-proposer-" + suffix, "Option Proposer " + suffix, "LECTURER", department);
        UserEntity facultyHead = saveUser("option-head-" + suffix, "Option Faculty Head " + suffix, "FACULTY_HEAD", department);
        UserEntity student = saveUser("option-student-" + suffix, "Option Student " + suffix, "STUDENT", department);
        UserEntity otherDepartmentSupervisor = saveUser(
                "option-other-" + suffix, "Option Other " + suffix, "LECTURER", otherDepartment);
        TopicEntity topic = topic(period, department, proposer, "options-" + suffix);

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("search", "options-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].supervisorOptions[*].id")
                        .value(org.hamcrest.Matchers.hasItem(facultyHead.getId().intValue())))
                .andExpect(jsonPath("$.topics[0].supervisorOptions[*].id")
                        .value(not(org.hamcrest.Matchers.hasItem(student.getId().intValue()))))
                .andExpect(jsonPath("$.topics[0].supervisorOptions[*].id")
                        .value(not(org.hamcrest.Matchers.hasItem(otherDepartmentSupervisor.getId().intValue()))));

        mockMvc.perform(put("/api/faculty/topics/{id}/supervisors", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"lecturerIds\":[" + facultyHead.getId() + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void supervisorListSupportsSearchSortAndPagination() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("page-admin-" + suffix, "Page Admin " + suffix, "ADMIN", null);
        DepartmentEntity firstDepartment = department("AAA-" + suffix);
        DepartmentEntity secondDepartment = department("ZZZ-" + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        UserEntity firstProposer = saveUser(
                "page-first-proposer-" + suffix, "Page First Proposer " + suffix, "LECTURER", firstDepartment);
        UserEntity secondProposer = saveUser(
                "page-second-proposer-" + suffix, "Page Second Proposer " + suffix, "LECTURER", secondDepartment);

        TopicEntity firstTopic = topic(period, firstDepartment, firstProposer, "a-0-" + suffix);
        topic(period, firstDepartment, firstProposer, "a-1-" + suffix);
        topic(period, firstDepartment, firstProposer, "a-2-" + suffix);
        topic(period, secondDepartment, secondProposer, "z-0-" + suffix);
        topic(period, secondDepartment, secondProposer, "z-1-" + suffix);
        TopicEntity lastTopic = topic(period, secondDepartment, secondProposer, "z-2-" + suffix);

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", suffix)
                        .param("sort", "department")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/supervisors"))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("6 topics")))
                .andExpect(content().string(containsString(lastTopic.getTitle())))
                .andExpect(content().string(not(containsString(firstTopic.getTitle()))))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("sort=department")))
                .andExpect(content().string(containsString("direction=asc")));

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", suffix)
                        .param("sort", "department")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.topics.length()").value(1))
                .andExpect(jsonPath("$.topics[0].title").value(lastTopic.getTitle()));

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("page", "99")
                        .param("size", "1")
                        .param("search", suffix)
                        .param("sort", "department")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.topics.length()").value(1));
    }

    @Test
    void supervisorListCombinesDepartmentPeriodStatusAndSearchFiltersAndPreservesState() throws Exception {
        String suffix = suffix();
        UserEntity admin = saveUser("filter-admin-" + suffix, "Filter Admin " + suffix, "ADMIN", null);
        DepartmentEntity matchingDepartment = department("FILTER-A-" + suffix);
        DepartmentEntity otherDepartment = department("FILTER-B-" + suffix);
        RegistrationPeriodEntity matchingPeriod = openPeriod("filter-a-" + suffix);
        RegistrationPeriodEntity otherPeriod = openPeriod("filter-b-" + suffix);
        UserEntity matchingProposer = saveUser("filter-proposer-a-" + suffix,
                "Filter Proposer A " + suffix, "LECTURER", matchingDepartment);
        UserEntity otherProposer = saveUser("filter-proposer-b-" + suffix,
                "Filter Proposer B " + suffix, "LECTURER", otherDepartment);
        TopicEntity match = topic(matchingPeriod, matchingDepartment, matchingProposer, "needle-match-" + suffix);
        match.setStatus(TopicStatus.PUBLISHED);
        topicRepository.saveAndFlush(match);
        TopicEntity wrongPeriod = topic(otherPeriod, matchingDepartment, matchingProposer, "needle-period-" + suffix);
        wrongPeriod.setStatus(TopicStatus.PUBLISHED);
        topicRepository.saveAndFlush(wrongPeriod);
        TopicEntity wrongDepartment = topic(matchingPeriod, otherDepartment, otherProposer, "needle-department-" + suffix);
        wrongDepartment.setStatus(TopicStatus.PUBLISHED);
        topicRepository.saveAndFlush(wrongDepartment);
        TopicEntity wrongStatus = topic(matchingPeriod, matchingDepartment, matchingProposer, "needle-status-" + suffix);

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("search", "needle")
                        .param("departmentId", matchingDepartment.getId().toString())
                        .param("periodId", matchingPeriod.getId().toString())
                        .param("status", "PUBLISHED")
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(match.getTitle())))
                .andExpect(content().string(not(containsString(wrongPeriod.getTitle()))))
                .andExpect(content().string(not(containsString(wrongDepartment.getTitle()))))
                .andExpect(content().string(not(containsString(wrongStatus.getTitle()))))
                .andExpect(content().string(containsString(otherDepartment.getName())))
                .andExpect(content().string(containsString(otherPeriod.getName())))
                .andExpect(content().string(containsString("departmentId=" + matchingDepartment.getId())))
                .andExpect(content().string(containsString("periodId=" + matchingPeriod.getId())))
                .andExpect(content().string(containsString("status=PUBLISHED")));

        mockMvc.perform(get("/api/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("search", "needle")
                        .param("departmentId", matchingDepartment.getId().toString())
                        .param("periodId", matchingPeriod.getId().toString())
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.topics[0].id").value(match.getId()));

        mockMvc.perform(get("/faculty/topics/supervisors")
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .param("departmentId", otherDepartment.getId().toString())
                        .param("periodId", otherPeriod.getId().toString())
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No data")))
                .andExpect(content().string(not(containsString("Assign supervisors"))))
                .andExpect(content().string(not(containsString("<table"))));
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
