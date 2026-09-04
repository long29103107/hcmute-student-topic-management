package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedUserVisitingLoginIsRedirectedToDashboard() throws Exception {
        mockMvc.perform(get("/login").with(user("student").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void roleProtectedRouteRejectsTheWrongRole() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404, 500, 503})
    void standardErrorPagesRedirectToNamedPaths(int errorStatus) throws Exception {
        String errorPath = errorPath(errorStatus);

        mockMvc.perform(get("/error")
                        .with(user("student").roles("STUDENT"))
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, errorStatus)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/test-error"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(errorPath));

        mockMvc.perform(get(errorPath).accept(MediaType.TEXT_HTML))
                .andExpect(status().is(errorStatus))
                .andExpect(content().string(containsString(String.valueOf(errorStatus))))
                .andExpect(content().string(containsString("/css/error.css")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sidebar"))));
    }

    @Test
    void forbiddenPageRedirectsToItsStandalonePath() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forbidden"));

        mockMvc.perform(get("/forbidden").accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("403")))
                .andExpect(content().string(containsString("/css/error.css")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sidebar"))));
    }

    @Test
    void browserAccessDeniedRedirectsToTheForbiddenPath() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user("student").roles("STUDENT"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forbidden"));
    }

    private static String errorPath(int errorStatus) {
        return switch (errorStatus) {
            case 400 -> "/bad-request";
            case 401 -> "/unauthorized";
            case 404 -> "/not-found";
            case 500 -> "/internal-server-error";
            case 503 -> "/service-unavailable";
            default -> throw new IllegalArgumentException("Unsupported error status: " + errorStatus);
        };
    }

    @Test
    void scoreApiIsReservedForLecturers() throws Exception {
        mockMvc.perform(post("/api/faculty/scores/1")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCapabilityRoutesStillRequireTheSpecificPermission() throws Exception {
        mockMvc.perform(get("/lecturer/topics").with(user("faculty-head").roles("FACULTY_HEAD")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorRoleCanPassFacultyAndStudentRouteGates() throws Exception {
        mockMvc.perform(get("/faculty/not-implemented").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/student/not-implemented").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void stateChangingLogoutRequiresCsrf() throws Exception {
        mockMvc.perform(post("/logout").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/logout").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"username\""))))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }
}
