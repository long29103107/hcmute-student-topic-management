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
                .andExpect(content().string(not(containsString(otherTopic.getTitle()))));
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
    void topicProposalApiReturnsOwnTopicsAndValidatesRequests() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = lecturer(suffix);
        DepartmentEntity department = department(suffix);
        RegistrationPeriodEntity period = openPeriod(suffix);
        topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "API proposal " + suffix, "API description"));

        mockMvc.perform(get("/api/lecturer/topics")
                        .with(user(lecturerPrincipal(lecturer.getEmailOrCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("API proposal " + suffix))
                .andExpect(jsonPath("$[0].statusCode").value("DRAFT"));

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
