package com.hcmute.topicmanagement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import static org.hamcrest.Matchers.containsString;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.StudentProfileRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class DatabaseSeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Test
    void onlyAdminCanOpenTheSeedPage() throws Exception {
        mockMvc.perform(get("/seed").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/seed").with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("seed"))
                .andExpect(content().string(containsString("POST /api/admin/seed")))
                .andExpect(content().string(containsString("data-seed-action")))
                .andExpect(content().string(containsString("Available seed APIs")))
                .andExpect(content().string(containsString("Seed data")));
    }

    @Test
    void onlyAdminCanResetAndReseedTheDatabase() throws Exception {
        departmentRepository.save(new DepartmentEntity("IT", "Information Technology"));

        mockMvc.perform(post("/api/admin/seed")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/seed")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(4))
                .andExpect(jsonPath("$.permissions").value(20))
                .andExpect(jsonPath("$.users").value(4))
                .andExpect(jsonPath("$.studentProfiles").value(1));

        org.assertj.core.api.Assertions.assertThat(roleRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(permissionRepository.count()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(studentProfileRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(departmentRepository.count()).isZero();

        var facultyHead = roleRepository.findByCode("FACULTY_HEAD").orElseThrow();
        List<String> lecturerPermissions = List.of("TOPIC_PROPOSE", "TOPIC_VIEW", "EVALUATION_SUBMIT", "RESULT_VIEW");
        lecturerPermissions.forEach(code -> org.assertj.core.api.Assertions.assertThat(
                rolePermissionRepository.existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode(code).orElseThrow().getId()))
                .as("Faculty Head should have lecturer permission %s", code)
                .isTrue());
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode("REGISTRATION_REVIEW").orElseThrow().getId()))
                .isTrue();
    }

    private static DatabaseUserPrincipal adminPrincipal() {
        return new DatabaseUserPrincipal(
                "admin",
                "",
                "System Administrator",
                "Administrator",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
