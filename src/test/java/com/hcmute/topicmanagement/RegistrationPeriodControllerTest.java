package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;
import com.hcmute.topicmanagement.service.RegistrationPeriodService;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private RegistrationPeriodService registrationPeriodService;

    @Test
    void facultyHeadCanCreateAndUpdatePeriodWithCreatorAndStatus() throws Exception {
        String suffix = suffix();
        String email = "period.head." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        UserEntity creator = new UserEntity("period-head-" + suffix, "Period Head " + suffix, "test-password-hash");
        creator.setEmailOrCode(email);
        creator = userRepository.saveAndFlush(creator);

        mockMvc.perform(get("/faculty/periods").with(user(facultyHead(email))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/periods"))
                .andExpect(content().string(containsString("Registration period management")));

        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", "Graduation thesis period " + suffix)
                        .param("type", "GRADUATION_THESIS")
                        .param("lecturerRegistrationStart", "2026-09-01T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-15T23:59")
                        .param("studentRegistrationStart", "2026-09-16T08:00")
                        .param("studentRegistrationEnd", "2026-09-30T23:59")
                        .param("reviewerScoreDeadline", "2026-10-15T23:59")
                        .param("councilReportDate", "2026-11-20T08:00")
                        .param("status", "OPEN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/periods"))
                .andExpect(flash().attribute("successMessage", "Registration period created successfully."));

        RegistrationPeriodEntity period = registrationPeriodRepository
                .findByNameIgnoreCase("Graduation thesis period " + suffix)
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(period.getType()).isEqualTo(PeriodType.GRADUATION_THESIS);
        org.assertj.core.api.Assertions.assertThat(period.getStatus()).isEqualTo(RegistrationPeriodStatus.OPEN);
        org.assertj.core.api.Assertions.assertThat(period.getCreatedBy().getId()).isEqualTo(creator.getId());
        org.assertj.core.api.Assertions.assertThat(period.getLecturerRegistrationStart())
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        org.assertj.core.api.Assertions.assertThat(period.getLecturerRegistrationEnd())
                .isEqualTo(LocalDateTime.of(2026, 9, 15, 23, 59));
        org.assertj.core.api.Assertions.assertThat(period.getStudentRegistrationStart())
                .isEqualTo(LocalDateTime.of(2026, 9, 16, 8, 0));
        org.assertj.core.api.Assertions.assertThat(period.getStudentRegistrationEnd())
                .isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59));
        org.assertj.core.api.Assertions.assertThat(period.getReviewerScoreDeadline()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(period.getCouncilReportDate()).isNotNull();

        mockMvc.perform(post("/faculty/periods/{id}/edit", period.getId())
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", "Updated thesis period " + suffix)
                        .param("type", "GRADUATION_THESIS")
                        .param("lecturerRegistrationStart", "2026-09-02T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-16T23:59")
                        .param("studentRegistrationStart", "2026-09-17T08:00")
                        .param("studentRegistrationEnd", "2026-10-01T23:59")
                        .param("reviewerScoreDeadline", "2026-10-16T23:59")
                        .param("councilReportDate", "2026-11-21T08:00")
                        .param("status", "CLOSED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Registration period updated successfully."));

        RegistrationPeriodEntity updated = registrationPeriodRepository.findById(period.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getName()).isEqualTo("Updated thesis period " + suffix);
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo(RegistrationPeriodStatus.CLOSED);
        org.assertj.core.api.Assertions.assertThat(updated.getLecturerRegistrationStart())
                .isEqualTo(LocalDateTime.of(2026, 9, 2, 8, 0));
        org.assertj.core.api.Assertions.assertThat(updated.getLecturerRegistrationEnd())
                .isEqualTo(LocalDateTime.of(2026, 9, 16, 23, 59));
        org.assertj.core.api.Assertions.assertThat(updated.getStudentRegistrationStart())
                .isEqualTo(LocalDateTime.of(2026, 9, 17, 8, 0));
        org.assertj.core.api.Assertions.assertThat(updated.getStudentRegistrationEnd())
                .isEqualTo(LocalDateTime.of(2026, 10, 1, 23, 59));

        mockMvc.perform(get("/faculty/periods").with(user(facultyHead(email))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Updated thesis period " + suffix)))
                .andExpect(content().string(containsString("CLOSED")))
                .andExpect(content().string(containsString("Period Head " + suffix)));
    }

    @Test
    void facultyHeadCanCreateDatnAndKltnPeriodTypes() throws Exception {
        String suffix = suffix();
        String email = "period.types." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-types-" + suffix, "Period Types " + suffix, email);

        createPeriod(email, "DATN period " + suffix, "COURSE", "DRAFT");
        createPeriod(email, "KLTN period " + suffix, "GRADUATION_THESIS", "OPEN");

        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository
                .findByNameIgnoreCase("DATN period " + suffix).orElseThrow().getType())
                .isEqualTo(PeriodType.COURSE);
        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository
                .findByNameIgnoreCase("KLTN period " + suffix).orElseThrow().getType())
                .isEqualTo(PeriodType.GRADUATION_THESIS);
    }

    @Test
    void periodStatusTransitionsAreForwardOnly() throws Exception {
        String suffix = suffix();
        String email = "period.status." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-status-" + suffix, "Period Status " + suffix, email);
        createPeriod(email, "Status period " + suffix, "COURSE", "DRAFT");
        RegistrationPeriodEntity period = registrationPeriodRepository
                .findByNameIgnoreCase("Status period " + suffix).orElseThrow();

        editPeriod(period.getId(), email, "Status period " + suffix, "CLOSED")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "Invalid registration period status transition: DRAFT -> CLOSED."));
        org.assertj.core.api.Assertions.assertThat(period.getStatus()).isEqualTo(RegistrationPeriodStatus.DRAFT);

        editPeriod(period.getId(), email, "Status period " + suffix, "OPEN")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Registration period updated successfully."));
        editPeriod(period.getId(), email, "Status period " + suffix, "DRAFT")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "Invalid registration period status transition: OPEN -> DRAFT."));

        editPeriod(period.getId(), email, "Status period " + suffix, "CLOSED")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Registration period updated successfully."));
        editPeriod(period.getId(), email, "Status period " + suffix, "ARCHIVED")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Registration period updated successfully."));
        editPeriod(period.getId(), email, "Status period " + suffix, "OPEN")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "Invalid registration period status transition: ARCHIVED -> OPEN."));

        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository.findById(period.getId()).orElseThrow().getStatus())
                .isEqualTo(RegistrationPeriodStatus.ARCHIVED);
    }

    @Test
    void readOnlyPeriodContractUsesInclusiveBoundariesAndStableErrors() {
        LocalDateTime lecturerStart = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime lecturerEnd = LocalDateTime.of(2026, 9, 15, 23, 59);
        LocalDateTime studentStart = LocalDateTime.of(2026, 9, 16, 8, 0);
        LocalDateTime studentEnd = LocalDateTime.of(2026, 9, 30, 23, 59);
        RegistrationPeriodEntity periodFixture = new RegistrationPeriodEntity(
                "Contract period " + suffix(), PeriodType.COURSE,
                lecturerStart, lecturerEnd, studentStart, studentEnd);
        periodFixture.setStatus(RegistrationPeriodStatus.OPEN);
        final RegistrationPeriodEntity period = registrationPeriodRepository.saveAndFlush(periodFixture);

        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), lecturerStart).isLecturerRegistrationOpen())
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), lecturerEnd).isLecturerRegistrationOpen())
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), lecturerStart.minusMinutes(1))
                        .isLecturerRegistrationOpen())
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), lecturerEnd.plusMinutes(1))
                        .isLecturerRegistrationOpen())
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), studentStart).isStudentRegistrationOpen())
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), studentEnd).isStudentRegistrationOpen())
                .isTrue();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> registrationPeriodService.requireOpenForLecturer(
                        period.getId(), lecturerEnd.plusMinutes(1)))
                .isInstanceOf(RegistrationPeriodService.RegistrationPeriodAccessException.class)
                .extracting("code")
                .isEqualTo(RegistrationPeriodService.LECTURER_WINDOW_CLOSED);

        period.setStatus(RegistrationPeriodStatus.CLOSED);
        registrationPeriodRepository.saveAndFlush(period);
        org.assertj.core.api.Assertions.assertThat(
                registrationPeriodService.inspect(period.getId(), lecturerStart).isStatusOpen())
                .isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> registrationPeriodService.requireOpenForStudent(period.getId(), studentStart))
                .isInstanceOf(RegistrationPeriodService.RegistrationPeriodAccessException.class)
                .extracting("code")
                .isEqualTo(RegistrationPeriodService.PERIOD_NOT_OPEN);
    }

    @Test
    void periodRulesRejectInvalidWindowsAndInapplicableMilestones() throws Exception {
        String suffix = suffix();
        String email = "period.rules." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-rules-" + suffix, "Period Rules " + suffix, email);

        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", "Missing field period " + suffix)
                        .param("type", "COURSE")
                        .param("lecturerRegistrationStart", "2026-09-01T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-15T23:59")
                        .param("studentRegistrationStart", "2026-09-16T08:00")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/periods"))
                .andExpect(content().string(containsString("Student registration end is required.")));

        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", "Invalid period " + suffix)
                        .param("type", "COURSE")
                        .param("lecturerRegistrationStart", "2026-09-15T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-01T23:59")
                        .param("studentRegistrationStart", "2026-09-16T08:00")
                        .param("studentRegistrationEnd", "2026-09-30T23:59")
                        .param("councilReportDate", "2026-11-20T08:00")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage",
                        "Lecturer registration start must be before or equal to the end."));

        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", "Invalid milestone " + suffix)
                        .param("type", "COURSE")
                        .param("lecturerRegistrationStart", "2026-09-01T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-15T23:59")
                        .param("studentRegistrationStart", "2026-09-16T08:00")
                        .param("studentRegistrationEnd", "2026-09-30T23:59")
                        .param("reviewerScoreDeadline", "2026-10-15T23:59")
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage",
                        "Reviewer score deadline only applies to graduation thesis periods."));
    }

    @Test
    void adminCanViewRegistrationPeriodsAndUsersWithoutPermissionCannotViewOrModify() throws Exception {
        mockMvc.perform(get("/faculty/periods").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/faculty/periods").with(user("faculty-head").roles("FACULTY_HEAD")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/faculty/periods")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .param("name", "Unauthorized period")
                        .param("type", "COURSE")
                        .param("status", "DRAFT"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/faculty/periods").with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/periods"));
    }

    @Test
    void periodMutationsRequireCsrf() throws Exception {
        String suffix = suffix();
        String email = "period.csrf." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-csrf-" + suffix, "Period Csrf " + suffix, email);

        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("name", "CSRF period " + suffix))
                .andExpect(status().isForbidden());
    }

    private void saveUser(String loginIdentifier, String fullName, String email) {
        UserEntity user = new UserEntity(loginIdentifier, fullName, "test-password-hash");
        user.setEmailOrCode(email);
        userRepository.saveAndFlush(user);
    }

    private void createPeriod(String email, String name, String type, String status) throws Exception {
        mockMvc.perform(post("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .with(csrf())
                        .param("name", name)
                        .param("type", type)
                        .param("lecturerRegistrationStart", "2026-09-01T08:00")
                        .param("lecturerRegistrationEnd", "2026-09-15T23:59")
                        .param("studentRegistrationStart", "2026-09-16T08:00")
                        .param("studentRegistrationEnd", "2026-09-30T23:59")
                        .param("status", status))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/faculty/periods"));
    }

    private org.springframework.test.web.servlet.ResultActions editPeriod(
            Long id, String email, String name, String status) throws Exception {
        return mockMvc.perform(post("/faculty/periods/{id}/edit", id)
                .with(user(facultyHead(email)))
                .with(csrf())
                .param("name", name)
                .param("type", "COURSE")
                .param("lecturerRegistrationStart", "2026-09-01T08:00")
                .param("lecturerRegistrationEnd", "2026-09-15T23:59")
                .param("studentRegistrationStart", "2026-09-16T08:00")
                .param("studentRegistrationEnd", "2026-09-30T23:59")
                .param("status", status));
    }

    private static DatabaseUserPrincipal facultyHead(String email) {
        return new DatabaseUserPrincipal(
                email,
                "",
                "Faculty Head",
                "Faculty Head",
                List.of(
                        new SimpleGrantedAuthority("ROLE_FACULTY_HEAD"),
                        new SimpleGrantedAuthority("PERIOD_MANAGE")));
    }

    private static DatabaseUserPrincipal adminPrincipal() {
        return new DatabaseUserPrincipal(
                "admin",
                "",
                "System Administrator",
                "Administrator",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("PERIOD_MANAGE")));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
