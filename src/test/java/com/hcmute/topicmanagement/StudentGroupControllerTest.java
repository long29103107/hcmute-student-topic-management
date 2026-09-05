package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class StudentGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void studentCanCreateAndViewOnlyRelatedGroups() throws Exception {
        String suffix = suffix();
        UserEntity student = student("group-owner-" + suffix, "Group Owner " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);

        String response = mockMvc.perform(post("/api/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alpha Team " + suffix + "\",\"periodId\":"
                                + period.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alpha Team " + suffix))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members[0].id").value(student.getId()))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response).contains("Alpha Team " + suffix);

        mockMvc.perform(get("/api/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.groups[*].name").value(hasItem("Alpha Team " + suffix)));

        UserEntity unrelatedStudent = student("group-unrelated-" + suffix, "Unrelated Student " + suffix);
        mockMvc.perform(get("/api/student/groups")
                        .with(user(studentPrincipal(unrelatedStudent.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.groups[*].name").value(not(hasItem("Alpha Team " + suffix))));

        mockMvc.perform(get("/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("student/groups"))
                .andExpect(content().string(containsString("Alpha Team " + suffix)))
                .andExpect(content().string(containsString("My groups")));
    }

    @Test
    void studentCanJoinAndLeaveAsNonLeader() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("group-leader-" + suffix, "Group Leader " + suffix);
        UserEntity member = student("group-member-" + suffix, "Group Member " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = saveGroup("Joinable Group " + suffix, period, leader, leader);

        mockMvc.perform(post("/api/student/groups/{id}/members", group.getId())
                        .with(user(studentPrincipal(member.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.members[*].id").value(hasItem(member.getId().intValue())));

        mockMvc.perform(delete("/api/student/groups/{id}/members/me", group.getId())
                        .with(user(studentPrincipal(member.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.members[*].id").value(not(hasItem(member.getId().intValue()))));

        org.assertj.core.api.Assertions.assertThat(studentGroupRepository.findByIdWithDetails(group.getId()).orElseThrow().getMembers())
                .extracting(UserEntity::getId)
                .containsExactly(leader.getId());
    }

    @Test
    void currentLeaderCanTransferLeadershipBeforeLeaving() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("transfer-leader-" + suffix, "Transfer Leader " + suffix);
        UserEntity member = student("transfer-member-" + suffix, "Transfer Member " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = saveGroup("Transfer Group " + suffix, period, leader, leader);
        join(group, member);

        mockMvc.perform(put("/api/student/groups/{id}/leader", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeaderId\":" + member.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderId").value(member.getId()))
                .andExpect(jsonPath("$.memberCount").value(2));

        mockMvc.perform(get("/student/groups")
                        .with(user(studentPrincipal(member.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transfer leader")));

        StudentGroupEntity reloaded = studentGroupRepository.findByIdWithDetails(group.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getLeader().getId()).isEqualTo(member.getId());
        org.assertj.core.api.Assertions.assertThat(reloaded.getMembers())
                .extracting(UserEntity::getId)
                .containsExactlyInAnyOrder(leader.getId(), member.getId());

        mockMvc.perform(delete("/api/student/groups/{id}/members/me", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(1));

        mockMvc.perform(delete("/api/student/groups/{id}/members/me", group.getId())
                        .with(user(studentPrincipal(member.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The group leader cannot leave without a leader transition."));
    }

    @Test
    void onlyTheCurrentLeaderCanTransferToAnActiveGroupMember() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("transfer-owner-" + suffix, "Transfer Owner " + suffix);
        UserEntity firstMember = student("transfer-first-" + suffix, "Transfer First " + suffix);
        UserEntity secondMember = student("transfer-second-" + suffix, "Transfer Second " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = saveGroup("Transfer Rules " + suffix, period, leader, leader);
        join(group, firstMember);
        join(group, secondMember);

        mockMvc.perform(put("/api/student/groups/{id}/leader", group.getId())
                        .with(user(studentPrincipal(firstMember.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeaderId\":" + secondMember.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STUDENT_GROUP_FORBIDDEN"));

        mockMvc.perform(put("/api/student/groups/{id}/leader", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeaderId\":999999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The new leader must be a member of this group."));
    }

    @Test
    void groupManageActorCanTransferLeadershipWithoutBeingAGroupMember() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("manager-transfer-leader-" + suffix, "Manager Transfer Leader " + suffix);
        UserEntity member = student("manager-transfer-member-" + suffix, "Manager Transfer Member " + suffix);
        UserEntity manager = account("group-manager-" + suffix, "Group Manager " + suffix, "ADMIN");
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = saveGroup("Manager Transfer " + suffix, period, leader, leader);
        join(group, member);

        mockMvc.perform(put("/api/student/groups/{id}/leader", group.getId())
                        .with(user(managerPrincipal(manager.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeaderId\":" + member.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderId").value(member.getId()));
    }

    @Test
    void completedAndInactiveGroupsRejectMembershipChanges() throws Exception {
        String suffix = suffix();
        UserEntity completedLeader = student("completed-leader-" + suffix, "Completed Leader " + suffix);
        UserEntity completedMember = student("completed-member-" + suffix, "Completed Member " + suffix);
        UserEntity inactiveLeader = student("inactive-leader-" + suffix, "Inactive Leader " + suffix);
        UserEntity joiner = student("inactive-joiner-" + suffix, "Inactive Joiner " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);

        StudentGroupEntity completedGroup = saveGroup(
                "Completed Group " + suffix, period, completedLeader, completedLeader);
        join(completedGroup, completedMember);
        completedGroup.setStatus(com.hcmute.topicmanagement.model.enums.GroupStatus.COMPLETED);
        studentGroupRepository.saveAndFlush(completedGroup);

        mockMvc.perform(delete("/api/student/groups/{id}/members/me", completedGroup.getId())
                        .with(user(studentPrincipal(completedMember.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only active groups can be joined, left or have their leader changed."));

        StudentGroupEntity inactiveGroup = saveGroup(
                "Inactive Group " + suffix, period, inactiveLeader, inactiveLeader);
        inactiveGroup.setStatus(com.hcmute.topicmanagement.model.enums.GroupStatus.INACTIVE);
        studentGroupRepository.saveAndFlush(inactiveGroup);

        mockMvc.perform(post("/api/student/groups/{id}/members", inactiveGroup.getId())
                        .with(user(studentPrincipal(joiner.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only active groups can be joined, left or have their leader changed."));
    }

    @Test
    void studentGroupRulesRejectDuplicateMemberLeaderLeaveAndOverCapacity() throws Exception {
        String suffix = suffix();
        UserEntity leader = student("rules-leader-" + suffix, "Rules Leader " + suffix);
        UserEntity firstMember = student("rules-first-" + suffix, "Rules First " + suffix);
        UserEntity secondMember = student("rules-second-" + suffix, "Rules Second " + suffix);
        UserEntity fourthMember = student("rules-fourth-" + suffix, "Rules Fourth " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity group = saveGroup("Rules Group " + suffix, period, leader, leader);

        join(group, firstMember);
        join(group, secondMember);

        mockMvc.perform(post("/api/student/groups/{id}/members", group.getId())
                        .with(user(studentPrincipal(firstMember.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STUDENT_GROUP_INVALID"))
                .andExpect(jsonPath("$.message").value("You are already a member of this group."));

        mockMvc.perform(post("/api/student/groups/{id}/members", group.getId())
                        .with(user(studentPrincipal(fourthMember.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A student group cannot have more than 3 members."));

        mockMvc.perform(delete("/api/student/groups/{id}/members/me", group.getId())
                        .with(user(studentPrincipal(leader.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "The group leader cannot leave without a leader transition."));
    }

    @Test
    void studentCanHaveOnlyOneActiveGroupInTheSamePeriod() throws Exception {
        String suffix = suffix();
        UserEntity student = student("one-group-" + suffix, "One Group Student " + suffix);
        UserEntity otherLeader = student("other-leader-" + suffix, "Other Leader " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        saveGroup("Existing Group " + suffix, period, student, student);
        StudentGroupEntity otherGroup = saveGroup("Other Group " + suffix, period, otherLeader, otherLeader);

        mockMvc.perform(post("/api/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second Group " + suffix + "\",\"periodId\":"
                                + period.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A student can belong to only one active group in the same registration period."));

        mockMvc.perform(post("/api/student/groups/{id}/members", otherGroup.getId())
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A student can belong to only one active group in the same registration period."));
    }

    @Test
    void groupListSupportsSearchSortPaginationAndEmptyState() throws Exception {
        String suffix = suffix();
        UserEntity student = student("group-page-" + suffix, "Group Page Student " + suffix);
        RegistrationPeriodEntity firstPeriod = openPeriod("A-" + suffix);
        RegistrationPeriodEntity secondPeriod = openPeriod("B-" + suffix);
        saveGroup("Beta Group " + suffix, firstPeriod, student, student);

        StudentGroupEntity secondGroup = saveGroup("Alpha Group " + suffix, secondPeriod, student, student);
        mockMvc.perform(get("/api/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .param("search", "Alpha")
                        .param("sort", "group")
                        .param("direction", "asc")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.groups[0].id").value(secondGroup.getId()))
                .andExpect(jsonPath("$.sort").value("group"))
                .andExpect(jsonPath("$.direction").value("asc"));

        UserEntity unrelatedStudent = student("group-empty-" + suffix, "Empty Student " + suffix);
        mockMvc.perform(get("/student/groups")
                        .with(user(studentPrincipal(unrelatedStudent.getEmailOrCode())))
                        .param("search", "missing"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No matching groups")))
                .andExpect(content().string(not(containsString("Showing"))));
    }

    @Test
    void onlyStudentWithGroupPermissionCanUseGroupEndpointsAndMutationsRequireCsrf() throws Exception {
        String suffix = suffix();
        UserEntity student = student("secured-group-" + suffix, "Secured Group Student " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);

        mockMvc.perform(get("/api/student/groups")
                        .with(user(student.getEmailOrCode()).roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/student/groups")
                        .with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/student/groups")
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No CSRF\",\"periodId\":" + period.getId() + "}"))
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(studentGroupRepository.findByMembers_Id(student.getId())).isEmpty();
    }

    @Test
    void concurrentJoinsStillAllowOnlyOneActiveGroupPerPeriod() throws Exception {
        String suffix = suffix();
        UserEntity firstLeader = student("concurrent-first-" + suffix, "Concurrent First " + suffix);
        UserEntity secondLeader = student("concurrent-second-" + suffix, "Concurrent Second " + suffix);
        UserEntity joiner = student("concurrent-joiner-" + suffix, "Concurrent Joiner " + suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        StudentGroupEntity firstGroup = saveGroup("Concurrent Group A " + suffix, period, firstLeader, firstLeader);
        StudentGroupEntity secondGroup = saveGroup("Concurrent Group B " + suffix, period, secondLeader, secondLeader);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> firstJoin = () -> concurrentJoin(
                    ready, start, firstGroup.getId(), joiner.getEmailOrCode());
            Callable<Integer> secondJoin = () -> concurrentJoin(
                    ready, start, secondGroup.getId(), joiner.getEmailOrCode());
            Future<Integer> firstResult = executor.submit(firstJoin);
            Future<Integer> secondResult = executor.submit(secondJoin);

            org.assertj.core.api.Assertions.assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            org.assertj.core.api.Assertions.assertThat(List.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder(200, 400);
            org.assertj.core.api.Assertions.assertThat(studentGroupRepository.findByMembers_Id(joiner.getId()))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int concurrentJoin(
            CountDownLatch ready, CountDownLatch start, Long groupId, String studentEmail) throws Exception {
        ready.countDown();
        org.assertj.core.api.Assertions.assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post("/api/student/groups/{id}/members", groupId)
                        .with(user(studentPrincipal(studentEmail)))
                        .with(csrf()))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void join(StudentGroupEntity group, UserEntity student) throws Exception {
        mockMvc.perform(post("/api/student/groups/{id}/members", group.getId())
                        .with(user(studentPrincipal(student.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    private StudentGroupEntity saveGroup(String name, RegistrationPeriodEntity period,
            UserEntity creator, UserEntity leader) {
        StudentGroupEntity group = new StudentGroupEntity(name, period, creator, leader);
        return studentGroupRepository.saveAndFlush(group);
    }

    private UserEntity student(String login, String fullName) {
        return account(login, fullName, "STUDENT");
    }

    private UserEntity account(String login, String fullName, String roleCode) {
        UserEntity student = new UserEntity(login, fullName, "test-password-hash");
        String domain = "STUDENT".equals(roleCode) ? "@student.hcmute.edu.vn" : "@hcmute.edu.vn";
        student.setEmailOrCode(login.toLowerCase(Locale.ROOT) + domain);
        student = userRepository.saveAndFlush(student);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        userRoleRepository.saveAndFlush(new UserRoleEntity(student, role));
        return student;
    }

    private RegistrationPeriodEntity openPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Group Period " + suffix, PeriodType.COURSE,
                now.minusHours(2), now.plusHours(2), now.minusHours(1), now.plusHours(2));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal studentPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Test student", "Student",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("GROUP_MANAGE")));
    }

    private static DatabaseUserPrincipal managerPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email, "", "Group Manager", "Administrator",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("GROUP_MANAGE")));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
