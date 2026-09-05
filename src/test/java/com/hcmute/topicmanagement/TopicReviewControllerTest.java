package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
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
class TopicReviewControllerTest {

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
    void facultyHeadAndAdminCanViewOnlyPendingTopicsInTheirReviewScope() throws Exception {
        String suffix = suffix();
        DepartmentEntity facultyDepartment = department("REVIEW-" + suffix);
        DepartmentEntity otherDepartment = department("OTHER-" + suffix);
        UserEntity facultyHead = saveUser("review-head-" + suffix, "Review Head " + suffix,
                "FACULTY_HEAD", facultyDepartment);
        UserEntity proposer = saveUser("review-proposer-" + suffix, "Review Proposer " + suffix,
                "LECTURER", facultyDepartment);
        UserEntity otherProposer = saveUser("review-other-proposer-" + suffix, "Other Proposer " + suffix,
                "LECTURER", otherDepartment);
        UserEntity admin = saveUser("review-admin-" + suffix, "Review Admin " + suffix, "ADMIN", null);
        RegistrationPeriodEntity period = openPeriod(suffix);

        TopicEntity ownPending = topic(period, facultyDepartment, proposer, "own-pending-" + suffix,
                TopicStatus.PENDING_APPROVAL);
        TopicEntity otherPending = topic(period, otherDepartment, otherProposer, "other-pending-" + suffix,
                TopicStatus.PENDING_APPROVAL);
        TopicEntity draft = topic(period, facultyDepartment, proposer, "draft-" + suffix, TopicStatus.DRAFT);

        mockMvc.perform(get("/faculty/topics/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/review"))
                .andExpect(content().string(containsString(ownPending.getTitle())))
                .andExpect(content().string(not(containsString(otherPending.getTitle()))))
                .andExpect(content().string(not(containsString(draft.getTitle()))));

        mockMvc.perform(get("/api/faculty/topics/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(ownPending.getTitle()))
                .andExpect(jsonPath("$[0].statusCode").value("PENDING_APPROVAL"));

        mockMvc.perform(get("/api/topics/review")
                        .with(user(adminPrincipal(admin.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.hasItems(
                        ownPending.getTitle(), otherPending.getTitle())))
                .andExpect(jsonPath("$[*].title").value(
                        not(org.hamcrest.Matchers.hasItem(draft.getTitle()))));
    }

    @Test
    void reviewerCanSearchSortAndPaginatePendingTopics() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("PAGE-" + suffix);
        UserEntity facultyHead = saveUser("page-review-head-" + suffix,
                "Page Review Head " + suffix, "FACULTY_HEAD", department);
        UserEntity proposer = saveUser("page-review-proposer-" + suffix,
                "Page Review Proposer " + suffix, "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix);
        for (int index = 0; index < 12; index++) {
            topic(period, department, proposer,
                    String.format("page review %02d %s", index, suffix), TopicStatus.PENDING_APPROVAL);
        }

        mockMvc.perform(get("/faculty/topics/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", suffix)
                        .param("sort", "topic")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/review"))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("Review topic page review 05 " + suffix)))
                .andExpect(content().string(not(containsString("Review topic page review 00 " + suffix))))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("sort=topic")))
                .andExpect(content().string(containsString("direction=asc")));

        mockMvc.perform(get("/faculty/topics/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .param("search", "page review 01 " + suffix))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Review topic page review 01 " + suffix)))
                .andExpect(content().string(not(containsString("Review topic page review 02 " + suffix))));
    }

    @Test
    void approveAndRejectUseThePendingApprovalTransitions() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("TRANSITION-" + suffix);
        UserEntity facultyHead = saveUser("transition-head-" + suffix, "Transition Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity proposer = saveUser("transition-proposer-" + suffix, "Transition Proposer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity approvedTopic = topic(period, department, proposer, "approve-" + suffix,
                TopicStatus.PENDING_APPROVAL);
        TopicEntity rejectedTopic = topic(period, department, proposer, "reject-" + suffix,
                TopicStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/faculty/topics/review")
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .param("topicId", approvedTopic.getId().toString())
                        .param("decision", "approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/topics/review"))
                .andExpect(flash().attribute("successMessage", "Topic proposal approved."));
        mockMvc.perform(post("/api/topics/{id}/approve", rejectedTopic.getId())
                        .with(user(facultyHeadPrincipal(facultyHead.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("REJECTED"))
                .andExpect(jsonPath("$.statusLabel").value("Rejected"));

        org.assertj.core.api.Assertions.assertThat(topicRepository.findById(approvedTopic.getId()).orElseThrow().getStatus())
                .isEqualTo(TopicStatus.APPROVED);
        org.assertj.core.api.Assertions.assertThat(topicRepository.findById(rejectedTopic.getId()).orElseThrow().getStatus())
                .isEqualTo(TopicStatus.REJECTED);
    }

    @Test
    void onlyPendingTopicsCanBeReviewedAndInvalidDecisionsAreRejected() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("INVALID-" + suffix);
        UserEntity admin = saveUser("invalid-admin-" + suffix, "Invalid Admin " + suffix, "ADMIN", null);
        UserEntity proposer = saveUser("invalid-proposer-" + suffix, "Invalid Proposer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity draft = topic(period, department, proposer, "invalid-draft-" + suffix, TopicStatus.DRAFT);
        TopicEntity approved = topic(period, department, proposer, "invalid-approved-" + suffix, TopicStatus.APPROVED);

        mockMvc.perform(post("/api/topics/{id}/approve", draft.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_REVIEW_INVALID"));

        mockMvc.perform(post("/api/topics/{id}/approve", approved.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Only pending approval topic proposals can be approved or rejected."));

        mockMvc.perform(post("/api/topics/{id}/approve", draft.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"PUBLISH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review decision must be approve or reject."));
    }

    @Test
    void proposerCannotApproveOwnTopicAndUnauthorizedUsersAreRejected() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("SECURITY-" + suffix);
        UserEntity facultyHead = saveUser("security-head-" + suffix, "Security Head " + suffix,
                "FACULTY_HEAD", department);
        UserEntity proposer = saveUser("security-proposer-" + suffix, "Security Proposer " + suffix,
                "FACULTY_HEAD", department);
        UserEntity lecturer = saveUser("security-lecturer-" + suffix, "Security Lecturer " + suffix,
                "LECTURER", department);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity ownTopic = topic(period, department, proposer, "own-" + suffix, TopicStatus.PENDING_APPROVAL);
        TopicEntity otherTopic = topic(period, department, proposer, "other-" + suffix, TopicStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/api/topics/{id}/approve", ownTopic.getId())
                        .with(user(facultyHeadPrincipal(proposer.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOPIC_REVIEW_FORBIDDEN"));

        mockMvc.perform(get("/faculty/topics/review")
                        .with(user(principal(lecturer.getEmailOrCode(), "ROLE_LECTURER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/topics/{id}/approve", otherTopic.getId())
                        .with(user(principal(lecturer.getEmailOrCode(), "ROLE_LECTURER")))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewMutationRequiresCsrf() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = department("CSRF-" + suffix);
        UserEntity admin = saveUser("csrf-admin-" + suffix, "CSRF Admin " + suffix, "ADMIN", null);
        UserEntity proposer = saveUser("csrf-proposer-" + suffix, "CSRF Proposer " + suffix,
                "LECTURER", department);
        TopicEntity topic = topic(openPeriod(suffix), department, proposer, "csrf-" + suffix,
                TopicStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/api/topics/{id}/approve", topic.getId())
                        .with(user(adminPrincipal(admin.getEmailOrCode())))
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    private TopicEntity topic(
            RegistrationPeriodEntity period, DepartmentEntity department, UserEntity proposer, String name,
            TopicStatus status) {
        TopicEntity topic = new TopicEntity(
                period, department, proposer, "Review topic " + name, "Review topic description");
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
        return principal(email, "ROLE_ADMIN", "TOPIC_REVIEW");
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(String email) {
        return principal(email, "ROLE_FACULTY_HEAD", "TOPIC_REVIEW");
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
