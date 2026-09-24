package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class TopicProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void lecturerCanCreateAndViewOnlyOwnTopicProposals() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);

        mockMvc.perform(post("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", "Smart campus platform " + suffix)
                        .param("description", "A proposal description for " + suffix)
                        .param("departmentId", department.getId().toString())
                        .param("periodId", period.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lecturer/topics"))
                .andExpect(flash().attribute("successMessage", "Topic proposal created successfully."));

        TopicEntity ownTopic = topicRepository.findByProposedBy_IdOrderByCreatedAtDesc(lecturer.getId()).stream()
                .filter(topic -> topic.getTitle().equals("Smart campus platform " + suffix))
                .findFirst()
                .orElseThrow();
        assertEquals(TopicStatus.DRAFT, ownTopic.getStatus());
        assertEquals(department.getId(), ownTopic.getDepartment().getId());
        assertEquals(period.getId(), ownTopic.getRegistrationPeriod().getId());

        UserEntity otherLecturer = lecturer("other-" + suffix);
        TopicEntity otherTopic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, otherLecturer, "Other private proposal " + suffix, "Other description"));

        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/topics"))
                .andExpect(content().string(containsString("Smart campus platform " + suffix)))
                .andExpect(content().string(containsString("Submit for review")))
                .andExpect(content().string(containsString("my-8 w-full max-w-2xl")))
                .andExpect(content().string(not(containsString(otherTopic.getTitle()))));

        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", "Smart campus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Smart campus platform " + suffix)))
                .andExpect(content().string(not(containsString(otherTopic.getTitle()))))
                .andExpect(content().string(containsString("value=\"smart campus\"")));
    }

    @Test
    void lecturerCannotCreateOutsideLecturerRegistrationWindow() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = closedPeriod(suffix);

        mockMvc.perform(post("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", "Closed window topic " + suffix)
                        .param("description", "This should be rejected")
                        .param("departmentId", department.getId().toString())
                        .param("periodId", period.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lecturer/topics"))
                .andExpect(flash().attribute(
                        "errorMessage", "The selected registration period is not open for lecturer proposals."));

        assertTrue(topicRepository.findByProposedBy_IdOrderByCreatedAtDesc(lecturer.getId()).isEmpty());
    }

    @Test
    void lecturerCanSortOwnTopicProposals() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer("sort-" + suffix);
        DepartmentEntity department = department("sort-" + suffix);
        RegistrationPeriodEntity period = openPeriod("sort-" + suffix);
        topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "Zulu topic " + suffix, "Zulu description"));
        topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "Alpha topic " + suffix, "Alpha description"));

        String html = mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("sort", "topic")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sort=topic")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(html.indexOf("Alpha topic " + suffix) < html.indexOf("Zulu topic " + suffix));
    }

    @Test
    void lecturerCanFilterOwnTopicProposalsByDepartmentStatusAndSearch() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer("filter-" + suffix);
        UserEntity otherLecturer = lecturer("filter-other-" + suffix);
        DepartmentEntity departmentA = department("FA-" + suffix);
        DepartmentEntity departmentB = department("FB-" + suffix);
        DepartmentEntity unusedActive = department("FU-" + suffix);
        DepartmentEntity inactive = department("FX-" + suffix);
        inactive.setActive(false);
        departmentRepository.saveAndFlush(inactive);
        RegistrationPeriodEntity period = openPeriod("F-" + suffix);
        topicRepository.saveAndFlush(new TopicEntity(
                period, departmentA, lecturer, "Draft A alpha " + suffix, "Draft A description"));
        TopicEntity pendingA = new TopicEntity(
                period, departmentA, lecturer, "Pending A alpha " + suffix, "Pending A description");
        pendingA.setStatus(TopicStatus.PENDING_APPROVAL);
        topicRepository.saveAndFlush(pendingA);
        topicRepository.saveAndFlush(new TopicEntity(
                period, departmentB, lecturer, "Draft B beta " + suffix, "Draft B description"));
        topicRepository.saveAndFlush(new TopicEntity(
                period, departmentA, otherLecturer, "Other A alpha " + suffix, "Other lecturer description"));

        // Directory header renders search, Department and Status controls; Department lists active departments.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"search\"")))
                .andExpect(content().string(containsString("name=\"departmentId\"")))
                .andExpect(content().string(containsString("name=\"status\"")))
                .andExpect(content().string(containsString("All departments")))
                .andExpect(content().string(containsString("All statuses")))
                .andExpect(content().string(containsString("Pending approval")))
                .andExpect(content().string(containsString(unusedActive.getCode())))
                .andExpect(content().string(not(containsString(inactive.getCode()))))
                .andExpect(content().string(not(containsString("Other A alpha " + suffix))));

        // Department filter: only the selected department, never another lecturer's proposal.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("departmentId", departmentA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft A alpha " + suffix)))
                .andExpect(content().string(containsString("Pending A alpha " + suffix)))
                .andExpect(content().string(not(containsString("Draft B beta " + suffix))))
                .andExpect(content().string(not(containsString("Other A alpha " + suffix))))
                .andExpect(content().string(containsString("departmentId=" + departmentA.getId())));

        // Status filter, then clearing it back to All.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pending A alpha " + suffix)))
                .andExpect(content().string(not(containsString("Draft A alpha " + suffix))))
                .andExpect(content().string(not(containsString("Draft B beta " + suffix))))
                .andExpect(content().string(containsString("status=PENDING_APPROVAL")));

        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("status", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft A alpha " + suffix)))
                .andExpect(content().string(containsString("Pending A alpha " + suffix)))
                .andExpect(content().string(containsString("Draft B beta " + suffix)));

        // Search matches the department and registration period shown in the directory.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", departmentB.getName()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft B beta " + suffix)))
                .andExpect(content().string(not(containsString("Draft A alpha " + suffix))));

        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", period.getName()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft A alpha " + suffix)))
                .andExpect(content().string(containsString("Draft B beta " + suffix)))
                .andExpect(content().string(not(containsString("Other A alpha " + suffix))));

        // Search + department + status combine with AND semantics and stay in the lecturer's scope.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", "alpha " + suffix)
                        .param("departmentId", departmentA.getId().toString())
                        .param("status", "draft"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft A alpha " + suffix)))
                .andExpect(content().string(not(containsString("Pending A alpha " + suffix))))
                .andExpect(content().string(not(containsString("Draft B beta " + suffix))))
                .andExpect(content().string(not(containsString("Other A alpha " + suffix))))
                .andExpect(content().string(containsString("status=DRAFT")))
                .andExpect(content().string(containsString("departmentId=" + departmentA.getId())));

        // Unknown status values fall back to All instead of failing the page.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Draft A alpha " + suffix)))
                .andExpect(content().string(containsString("Draft B beta " + suffix)));

        // No-match state: shared empty state, a way back, and no stale row actions or edit modals.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("departmentId", departmentB.getId().toString())
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No data")))
                .andExpect(content().string(containsString("Clear search and filters")))
                .andExpect(content().string(not(containsString("Draft B beta " + suffix))))
                .andExpect(content().string(not(containsString("topic-actions-menu-"))))
                .andExpect(content().string(not(containsString("submit-topic-form-"))))
                .andExpect(content().string(not(containsString("edit-topic-modal-"))))
                .andExpect(content().string(not(containsString("Topic proposal pagination"))));
    }

    @Test
    void lecturerTopicDirectoryPaginatesClampsPagesAndPreservesQueryState() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer("paging-" + suffix);
        DepartmentEntity department = department("PG-" + suffix);
        RegistrationPeriodEntity period = openPeriod("PG-" + suffix);
        for (int index = 1; index <= 7; index++) {
            topicRepository.saveAndFlush(new TopicEntity(
                    period, department, lecturer, String.format("Paged topic %02d %s", index, suffix),
                    "Paged description"));
        }
        String searchValue = suffix.toLowerCase(Locale.ROOT);

        // Page 2 of 2 with every query parameter preserved in the pagination links.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", suffix)
                        .param("departmentId", department.getId().toString())
                        .param("status", "DRAFT")
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("size", "5")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Paged topic 06 " + suffix)))
                .andExpect(content().string(containsString("Paged topic 07 " + suffix)))
                .andExpect(content().string(not(containsString("Paged topic 01 " + suffix))))
                .andExpect(content().string(containsString("Topic proposal pagination")))
                .andExpect(content().string(containsString("page=0")))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("search=" + searchValue)))
                .andExpect(content().string(containsString("departmentId=" + department.getId())))
                .andExpect(content().string(containsString("status=DRAFT")))
                .andExpect(content().string(containsString("sort=topic")))
                .andExpect(content().string(containsString("direction=asc")))
                // Active ascending column shows the ascending indicator.
                .andExpect(content().string(containsString("↑")));

        // Out-of-range pages clamp to the last and first pages.
        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", suffix)
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("size", "5")
                        .param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Paged topic 07 " + suffix)))
                .andExpect(content().string(not(containsString("Paged topic 01 " + suffix))));

        mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", suffix)
                        .param("sort", "title")
                        .param("direction", "asc")
                        .param("size", "5")
                        .param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Paged topic 01 " + suffix)))
                .andExpect(content().string(not(containsString("Paged topic 07 " + suffix))));

        // Descending sort reverses the order and shows the descending indicator.
        String html = mockMvc.perform(get("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .param("search", suffix)
                        .param("sort", "topic")
                        .param("direction", "desc")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("↓")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(html.indexOf("Paged topic 07 " + suffix) < html.indexOf("Paged topic 01 " + suffix));
    }

    @Test
    void invalidTopicProposalFormIsRejected() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);

        mockMvc.perform(post("/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", " ")
                        .param("description", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("lecturer/topics"))
                .andExpect(content().string(containsString("Topic title is required.")))
                .andExpect(content().string(containsString("Topic description is required.")))
                .andExpect(content().string(containsString("Select a department.")))
                .andExpect(content().string(containsString("Select a registration period.")));
    }

    @Test
    void lecturerCanUpdateOwnDraftButCannotUpdateAnotherOrSubmittedTopic() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        UserEntity otherLecturer = lecturer("other-update-" + suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity ownTopic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "Original title " + suffix, "Original description"));
        TopicEntity otherTopic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, otherLecturer, "Other title " + suffix, "Other description"));

        mockMvc.perform(post("/lecturer/topics/{id}/edit", ownTopic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", "Updated title " + suffix)
                        .param("description", "Updated description")
                        .param("departmentId", department.getId().toString())
                        .param("periodId", period.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Topic proposal updated successfully."));

        assertEquals("Updated title " + suffix, topicRepository.findById(ownTopic.getId()).orElseThrow().getTitle());

        mockMvc.perform(post("/lecturer/topics/{id}/edit", otherTopic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", "Should not update")
                        .param("description", "Should not update")
                        .param("departmentId", department.getId().toString())
                        .param("periodId", period.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Topic proposal not found: " + otherTopic.getId()));

        ownTopic.setStatus(TopicStatus.PENDING_APPROVAL);
        topicRepository.saveAndFlush(ownTopic);
        mockMvc.perform(post("/lecturer/topics/{id}/edit", ownTopic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .param("title", "Submitted update")
                        .param("description", "Should not update")
                        .param("departmentId", department.getId().toString())
                        .param("periodId", period.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "Only draft or rejected topic proposals can be edited."));
    }

    @Test
    void lecturerCanSubmitDraftAndRejectedTopicForReview() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "Submit topic " + suffix, "Submit description"));

        mockMvc.perform(post("/lecturer/topics/{id}/submit", topic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lecturer/topics"))
                .andExpect(flash().attribute(
                        "successMessage", "Topic proposal submitted for faculty review."));

        assertEquals(TopicStatus.PENDING_APPROVAL, topicRepository.findById(topic.getId()).orElseThrow().getStatus());

        topic.setStatus(TopicStatus.REJECTED);
        topicRepository.saveAndFlush(topic);

        mockMvc.perform(post("/lecturer/topics/{id}/submit", topic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "successMessage", "Topic proposal submitted for faculty review."));

        assertEquals(TopicStatus.PENDING_APPROVAL, topicRepository.findById(topic.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/lecturer/topics/{id}/submit", topic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "Only draft or rejected topic proposals can be submitted for review."));
    }

    @Test
    void topicProposalApiReturnsOwnTopicsAndValidatesRequests() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        TopicEntity apiTopic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "API proposal " + suffix, "API description"));

        mockMvc.perform(get("/api/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("API proposal " + suffix))
                .andExpect(jsonPath("$[0].statusCode").value("DRAFT"));

        mockMvc.perform(post("/api/lecturer/topics/{id}/submit", apiTopic.getId())
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"description\":\"\",\"departmentId\":null,\"periodId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_PROPOSAL_INVALID"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Topic title is required."));
    }

    @Test
    void topicProposalRoutesRequireProposalPermission() throws Exception {
        mockMvc.perform(get("/lecturer/topics").with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lecturer/topics").with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isForbidden());
    }

    private UserEntity lecturer(String suffix) {
        UserEntity lecturer = new UserEntity(
                "topic-lecturer-" + suffix,
                "Topic Lecturer " + suffix,
                "test-password-hash");
        lecturer.setEmailOrCode("topic-lecturer-" + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn");
        return userRepository.saveAndFlush(lecturer);
    }

    private DepartmentEntity department(String suffix) {
        return departmentRepository.saveAndFlush(
                new DepartmentEntity("TOPIC-" + suffix, "Topic Department " + suffix));
    }

    private RegistrationPeriodEntity openPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Open Topic Period " + suffix,
                PeriodType.COURSE,
                now.minusHours(1),
                now.plusHours(1),
                now.minusHours(1),
                now.plusHours(1));
        period.setStatus(RegistrationPeriodStatus.OPEN);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private RegistrationPeriodEntity closedPeriod(String suffix) {
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                "Closed Topic Period " + suffix,
                PeriodType.COURSE,
                now.minusHours(2),
                now.minusHours(1),
                now.minusHours(2),
                now.minusHours(1));
        period.setStatus(RegistrationPeriodStatus.CLOSED);
        return registrationPeriodRepository.saveAndFlush(period);
    }

    private static DatabaseUserPrincipal lecturerPrincipal(String email) {
        return new DatabaseUserPrincipal(
                email,
                "",
                "Topic Lecturer",
                "Lecturer",
                List.of(
                        new SimpleGrantedAuthority("ROLE_LECTURER"),
                        new SimpleGrantedAuthority("TOPIC_PROPOSE")));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
