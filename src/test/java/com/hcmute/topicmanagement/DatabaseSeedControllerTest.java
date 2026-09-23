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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.AnnouncementRepository;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseSeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private RegistrationResultRepository registrationResultRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void onlyAdminCanOpenTheSeedPage() throws Exception {
        mockMvc.perform(get("/seed").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/seed").with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("seed"))
                .andExpect(content().string(containsString("POST /api/seed/ddl")))
                .andExpect(content().string(containsString("POST /api/seed/permissions")))
                .andExpect(content().string(containsString("POST /api/seed/roles")))
                .andExpect(content().string(containsString("POST /api/seed/users")))
                .andExpect(content().string(containsString("POST /api/seed/departments")))
                .andExpect(content().string(containsString("POST /api/seed/registration-periods")))
                .andExpect(content().string(containsString("POST /api/seed/student-groups")))
                .andExpect(content().string(containsString("POST /api/seed/topics")))
                .andExpect(content().string(containsString("POST /api/seed/topic-registrations")))
                .andExpect(content().string(containsString("POST /api/seed/review-boards")))
                .andExpect(content().string(containsString("POST /api/seed/announcements")))
                .andExpect(content().string(containsString("data-seed-action")))
                .andExpect(content().string(containsString("Available seed APIs")))
                .andExpect(content().string(containsString("Seed data")));
    }

    @Test
    void seedPipelineApisAreAdminOnlyAndRunInOrder() throws Exception {
        List<String> endpoints = List.of(
                "/api/seed/ddl",
                "/api/seed/permissions",
                "/api/seed/roles",
                "/api/seed/role-permissions",
                "/api/seed/departments",
                "/api/seed/users",
                "/api/seed/registration-periods",
                "/api/seed/student-groups",
                "/api/seed/topics",
                "/api/seed/topic-registrations",
                "/api/seed/review-boards",
                "/api/seed/announcements");

        for (String endpoint : endpoints) {
            mockMvc.perform(post(endpoint)
                            .with(user("student").roles("STUDENT"))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        for (String endpoint : endpoints) {
            mockMvc.perform(post(endpoint)
                            .with(user(adminPrincipal()))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/seed/student-profiles")
                        .with(user(adminPrincipal()))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        org.assertj.core.api.Assertions.assertThat(roleRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(permissionRepository.count()).isEqualTo(28);
        org.assertj.core.api.Assertions.assertThat(departmentRepository.count()).isEqualTo(16);
        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(studentGroupRepository.count()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(topicRepository.count()).isEqualTo(90);
        org.assertj.core.api.Assertions.assertThat(announcementRepository.count()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(announcementRepository.findAll().stream()
                .filter(announcement -> announcement.getStatus() == AnnouncementStatus.PUBLISHED)
                .count()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(announcementRepository.findAll().stream()
                .filter(announcement -> announcement.getStatus() == AnnouncementStatus.DRAFT)
                .count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(registrationResultRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(topicRepository.countSupervisorAssignments()).isEqualTo(95);
        org.assertj.core.api.Assertions.assertThat(topicRepository.findAllForSupervisorManagement())
                .hasSize(90)
                .allSatisfy(topic -> org.assertj.core.api.Assertions.assertThat(topic.getSupervisors())
                        .hasSizeBetween(1, 2));
        var topicReviewPermission = permissionRepository.findByCode("TOPIC_REVIEW").orElseThrow();
        var announcementManagePermission = permissionRepository.findByCode("ANNOUNCEMENT_MANAGE").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(topicRepository
                .findByStatusOrderByCreatedAtDesc(com.hcmute.topicmanagement.model.enums.TopicStatus.PENDING_APPROVAL))
                .hasSize(10);
        for (TopicStatus topicStatus : TopicStatus.values()) {
            int expectedCount = topicStatus == TopicStatus.PUBLISHED
                    ? 40
                    : topicStatus == TopicStatus.DRAFT ? 20 : 10;
            org.assertj.core.api.Assertions.assertThat(topicRepository
                    .findByStatusOrderByCreatedAtDesc(topicStatus))
                    .as("Seed should contain %s topics with status %s", expectedCount, topicStatus)
                    .hasSize(expectedCount);
        }
        var adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        var facultyHeadRole = roleRepository.findByCode("FACULTY_HEAD").orElseThrow();
        var lecturerRole = roleRepository.findByCode("LECTURER").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(adminRole.getId(), topicReviewPermission.getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(facultyHeadRole.getId(), topicReviewPermission.getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(lecturerRole.getId(), topicReviewPermission.getId()))
                .isFalse();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(adminRole.getId(), announcementManagePermission.getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(facultyHeadRole.getId(), announcementManagePermission.getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(lecturerRole.getId(), announcementManagePermission.getId()))
                .isFalse();
        var studentRole = roleRepository.findByCode("STUDENT").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(studentRole.getId(), announcementManagePermission.getId()))
                .isFalse();
        topicRepository.findAllForSupervisorManagement().forEach(topic ->
                org.assertj.core.api.Assertions.assertThat(topic.getSupervisors())
                        .allSatisfy(supervisor -> org.assertj.core.api.Assertions.assertThat(
                                supervisor.getDepartment().getId())
                                .isEqualTo(topic.getDepartment().getId())));
        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository.findAll().get(0).getStatus())
                .isEqualTo(RegistrationPeriodStatus.OPEN);
        org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(79);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("FACULTY_HEAD"))
                .isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("LECTURER"))
                .isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("STUDENT"))
                .isEqualTo(53);
        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmailIgnoreCase("admin@hcmute.edu.vn"))
                .isPresent();
        org.assertj.core.api.Assertions.assertThat(userRepository
                .findByEmailIgnoreCase("nguyen.thanh.binh@lecturer.hcmute.edu.vn"))
                .isPresent();
        org.assertj.core.api.Assertions.assertThat(userRepository
                .findByEmailIgnoreCase("24110000@student.hcmute.edu.vn"))
                .isPresent();
        List<String> departments = List.of("CNTT", "KHMT", "CNPM", "HTTT");
        int[] studentCounts = {13, 13, 12, 12};
        for (int index = 0; index < departments.size(); index++) {
            String department = departments.get(index);
            org.assertj.core.api.Assertions.assertThat(userRepository
                    .countActiveByDepartmentCodeAndRoleCode(department, "FACULTY_HEAD"))
                    .isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(userRepository
                    .countActiveByDepartmentCodeAndRoleCode(department, "LECTURER"))
                    .isEqualTo(4);
            org.assertj.core.api.Assertions.assertThat(userRepository
                    .countActiveByDepartmentCodeAndRoleCode(department, "STUDENT"))
                    .isEqualTo(studentCounts[index]);
        }
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches(
                "admin123", userRepository.findByLoginIdentifier("admin").orElseThrow().getPasswordHash()))
                .isTrue();
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
                .andExpect(jsonPath("$.tablesReset").value(18))
                .andExpect(jsonPath("$.roles").value(4))
                .andExpect(jsonPath("$.permissions").value(28))
                .andExpect(jsonPath("$.departments").value(16))
                .andExpect(jsonPath("$.users").value(79))
                .andExpect(jsonPath("$.registrationPeriods").value(1))
                .andExpect(jsonPath("$.studentGroups").value(20))
                .andExpect(jsonPath("$.topics").value(90))
                .andExpect(jsonPath("$.topicSupervisors").value(95))
                .andExpect(jsonPath("$.facultyHeads").value(5))
                .andExpect(jsonPath("$.lecturers").value(20))
                .andExpect(jsonPath("$.students").value(53))
                .andExpect(jsonPath("$.announcements").value(3))
                .andExpect(jsonPath("$.registrationResults").value(1))
                .andExpect(jsonPath("$.studentProfiles").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(roleRepository.count()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(permissionRepository.count()).isEqualTo(28);
        org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(79);
        org.assertj.core.api.Assertions.assertThat(departmentRepository.count()).isEqualTo(16);
        org.assertj.core.api.Assertions.assertThat(registrationPeriodRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(studentGroupRepository.count()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(topicRepository.count()).isEqualTo(90);
        org.assertj.core.api.Assertions.assertThat(announcementRepository.count()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(registrationResultRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(topicRepository.countSupervisorAssignments()).isEqualTo(95);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("FACULTY_HEAD"))
                .isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("LECTURER"))
                .isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(userRoleRepository.countByRole_CodeAndActiveTrueAndUser_ActiveTrue("STUDENT"))
                .isEqualTo(53);

        var facultyHead = roleRepository.findByCode("FACULTY_HEAD").orElseThrow();
        List<String> lecturerPermissions = List.of(
                "TOPIC_PROPOSE", "TOPIC_VIEW", "REPORT_VIEW", "EVALUATION_SUBMIT", "RESULT_VIEW");
        lecturerPermissions.forEach(code -> org.assertj.core.api.Assertions.assertThat(
                rolePermissionRepository.existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode(code).orElseThrow().getId()))
                .as("Faculty Head should have lecturer permission %s", code)
                .isTrue());
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode("REGISTRATION_REVIEW").orElseThrow().getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode("SUPERVISOR_MANAGE").orElseThrow().getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode("GROUP_READ").orElseThrow().getId()))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHead.getId(), permissionRepository.findByCode("GROUP_UPDATE").orElseThrow().getId()))
                .isTrue();

        var admin = roleRepository.findByCode("ADMIN").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository.countByRole_IdAndActiveTrue(admin.getId()))
                .isEqualTo(permissionRepository.count());
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        admin.getId(), permissionRepository.findByCode("DEPARTMENT_MANAGE").orElseThrow().getId()))
                .isTrue();

        var studentRole = roleRepository.findByCode("STUDENT").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        studentRole.getId(), permissionRepository.findByCode("GROUP_MANAGE").orElseThrow().getId()))
                .isTrue();

        var facultyHeadRole = roleRepository.findByCode("FACULTY_HEAD").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(rolePermissionRepository
                .existsByRole_IdAndPermission_IdAndActiveTrue(
                        facultyHeadRole.getId(), permissionRepository.findByCode("DEPARTMENT_MANAGE").orElseThrow().getId()))
                .isFalse();
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
