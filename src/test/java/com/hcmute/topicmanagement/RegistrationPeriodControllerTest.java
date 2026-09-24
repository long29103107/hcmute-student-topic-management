package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:registrationperiodtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
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
    void facultyHeadCanSearchSortAndPaginateRegistrationPeriods() throws Exception {
        String suffix = suffix();
        String email = "period.page." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-page-" + suffix, "Period Page User " + suffix, email);
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 30, 23, 59);
        for (int index = 0; index < 12; index++) {
            registrationPeriodRepository.saveAndFlush(new RegistrationPeriodEntity(
                    String.format("Period page %02d %s", index, suffix), PeriodType.COURSE,
                    start, end, start, end));
        }

        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", suffix)
                        .param("sort", "period")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/periods"))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("Period page 06 " + suffix)))
                .andExpect(content().string(not(containsString("Period page 11 " + suffix))))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("sort=period")))
                .andExpect(content().string(containsString("direction=desc")));

        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", "page 01 " + suffix))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Period page 01 " + suffix)))
                .andExpect(content().string(not(containsString("Period page 02 " + suffix))));
    }

    @Test
    void facultyHeadCanFilterRegistrationPeriodsByEachStatusAndSearch() throws Exception {
        String suffix = suffix();
        String email = "period.status." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-status-" + suffix, "Period Status User " + suffix, email);
        periodWithStatus("Status draft alpha " + suffix, RegistrationPeriodStatus.DRAFT);
        periodWithStatus("Status open alpha " + suffix, RegistrationPeriodStatus.OPEN);
        periodWithStatus("Status open beta " + suffix, RegistrationPeriodStatus.OPEN);
        periodWithStatus("Status closed alpha " + suffix, RegistrationPeriodStatus.CLOSED);
        periodWithStatus("Status archived alpha " + suffix, RegistrationPeriodStatus.ARCHIVED);
        List<String> names = List.of(
                "Status draft alpha " + suffix, "Status open alpha " + suffix, "Status open beta " + suffix,
                "Status closed alpha " + suffix, "Status archived alpha " + suffix);

        // The Status dropdown sits in the search form with All plus every period status.
        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", suffix))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"period-status-filter\"")))
                .andExpect(content().string(containsString("All statuses")))
                .andExpect(content().string(containsString("value=\"ARCHIVED\"")));

        // Each status returns only periods with that status.
        for (RegistrationPeriodStatus periodStatus : RegistrationPeriodStatus.values()) {
            String marker = "Status " + periodStatus.name().toLowerCase(Locale.ROOT) + " ";
            var result = mockMvc.perform(get("/faculty/periods")
                            .with(user(facultyHead(email)))
                            .param("search", suffix)
                            .param("status", periodStatus.name()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("status=" + periodStatus.name())));
            for (String name : names) {
                result.andExpect(content().string(name.startsWith(marker)
                        ? containsString(name)
                        : not(containsString(name))));
            }
        }

        // Clearing the status (All) returns every period again.
        var all = mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", suffix)
                        .param("status", ""))
                .andExpect(status().isOk());
        for (String name : names) {
            all.andExpect(content().string(containsString(name)));
        }

        // Search + Status use AND semantics.
        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", "beta " + suffix)
                        .param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Status open beta " + suffix)))
                .andExpect(content().string(not(containsString("Status open alpha " + suffix))));

        // Empty filtered result: shared no-data state, a clear link and no stale period actions.
        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", "beta " + suffix)
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No data")))
                .andExpect(content().string(containsString("Clear search and filters")))
                .andExpect(content().string(not(containsString("Status open beta " + suffix))))
                .andExpect(content().string(not(containsString("edit-period-modal-"))))
                .andExpect(content().string(not(containsString("Registration period pagination"))));

        // Unknown values fall back to All statuses, like the Faculty student groups directory.
        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", suffix)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Status open alpha " + suffix)))
                .andExpect(content().string(containsString("Status closed alpha " + suffix)));
    }

    @Test
    void periodStatusFilterIsPreservedAcrossSortAndPaginationAndRequiresPermission() throws Exception {
        String suffix = suffix();
        String email = "period.status.page." + suffix.toLowerCase(Locale.ROOT) + "@lecturer.hcmute.edu.vn";
        saveUser("period-status-page-" + suffix, "Period Status Page User " + suffix, email);
        for (int index = 1; index <= 7; index++) {
            periodWithStatus(String.format("Status page %02d %s", index, suffix), RegistrationPeriodStatus.OPEN);
        }
        periodWithStatus("Status page 00 closed " + suffix, RegistrationPeriodStatus.CLOSED);
        String searchValue = suffix.toLowerCase(Locale.ROOT);

        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", suffix)
                        .param("status", "OPEN")
                        .param("sort", "name")
                        .param("direction", "asc")
                        .param("size", "5")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Status page 06 " + suffix)))
                .andExpect(content().string(containsString("Status page 07 " + suffix)))
                .andExpect(content().string(not(containsString("Status page 01 " + suffix))))
                .andExpect(content().string(not(containsString("Status page 00 closed " + suffix))))
                .andExpect(content().string(containsString("Registration period pagination")))
                .andExpect(content().string(containsString("status=OPEN")))
                .andExpect(content().string(containsString("search=" + searchValue)))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("sort=period")))
                .andExpect(content().string(containsString("direction=asc")));

        // Out-of-range page clamps to the last filtered page.
        mockMvc.perform(get("/faculty/periods")
                        .with(user(facultyHead(email)))
                        .param("search", suffix)
                        .param("status", "OPEN")
                        .param("size", "5")
                        .param("page", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Status page 07 " + suffix)))
                .andExpect(content().string(not(containsString("Status page 01 " + suffix))));

        // The filter does not widen access: users without PERIOD_MANAGE are still rejected.
        mockMvc.perform(get("/faculty/periods")
                        .with(user("student").roles("STUDENT"))
                        .param("status", "OPEN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/faculty/periods")
                        .with(user("faculty-head").roles("FACULTY_HEAD"))
                        .param("status", "OPEN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/faculty/periods")
                        .with(user(adminPrincipal()))
                        .param("status", "OPEN"))
                .andExpect(status().isOk());
    }

    private void periodWithStatus(String name, RegistrationPeriodStatus periodStatus) {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 30, 23, 59);
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                name, PeriodType.COURSE, start, end, start, end);
        period.setStatus(periodStatus);
        registrationPeriodRepository.saveAndFlush(period);
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
