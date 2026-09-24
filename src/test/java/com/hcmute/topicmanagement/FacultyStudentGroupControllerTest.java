package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.service.DatabaseSeedService;

@SpringBootTest
@AutoConfigureMockMvc
class FacultyStudentGroupControllerTest {

    private static final String CNTT_HEAD = "nguyen.van.khang@lecturer.hcmute.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseSeedService databaseSeedService;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void resetSeed() {
        databaseSeedService.resetAndSeed();
    }

    @Test
    void facultyHeadSeesOnlyDepartmentGroupsAndDirectoryControls() throws Exception {
        mockMvc.perform(get("/faculty/groups")
                        .param("search", "Phoenix")
                        .param("sort", "leader")
                        .param("direction", "desc")
                        .param("size", "10")
                        .with(user(facultyHeadPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/groups"))
                .andExpect(content().string(containsString("Student groups")))
                .andExpect(content().string(containsString("Nhóm Phoenix")))
                .andExpect(content().string(not(containsString("Nhóm Orion"))))
                .andExpect(content().string(containsString("placeholder=\"Search groups\"")))
                .andExpect(content().string(containsString("name=\"sort\"")))
                .andExpect(content().string(containsString("name=\"direction\"")))
                .andExpect(content().string(containsString("name=\"size\"")))
                .andExpect(content().string(containsString("aria-label=\"Edit group\"")))
                .andExpect(content().string(containsString("group-detail-modal-")));
    }

    @Test
    void adminSeesAllGroupsAndMissingReadPermissionIsRejected() throws Exception {
        mockMvc.perform(get("/faculty/groups")
                        .param("size", "100")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nhóm Phoenix")))
                .andExpect(content().string(containsString("Nhóm Nova")))
                .andExpect(content().string(containsString("Nhóm Atlas")));

        mockMvc.perform(get("/faculty/groups")
                        .with(user(userWithoutGroupRead())))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupDetailsRenderMembersAndEnforceFacultyScope() throws Exception {
        Long cnttGroupId = groupId("Nhóm Phoenix");
        mockMvc.perform(get("/faculty/groups/{id}", cnttGroupId)
                        .with(user(facultyHeadPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/group-detail"))
                .andExpect(content().string(containsString("Student group details")))
                .andExpect(content().string(containsString("Nhóm Phoenix")))
                .andExpect(content().string(containsString("Group members")));

        Long otherDepartmentGroupId = groupId("Nhóm Nova");
        mockMvc.perform(get("/faculty/groups/{id}", otherDepartmentGroupId)
                        .with(user(facultyHeadPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/forbidden"));
    }

    @Test
    void authorizedFacultyHeadCanEditGroupAndReadOnlyAccountCannot() throws Exception {
        Long groupId = groupId("Nhóm Phoenix");
        Long leaderId = userRepository.findByLoginIdentifier("24110001").orElseThrow().getId();

        mockMvc.perform(post("/faculty/groups/{id}/edit", groupId)
                        .param("name", "Nhóm Phoenix Updated")
                        .param("leaderId", leaderId.toString())
                        .param("status", "ACTIVE")
                        .with(user(facultyHeadPrincipal()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern(
                        "/faculty/groups?*"));

        org.assertj.core.api.Assertions.assertThat(studentGroupRepository.findById(groupId).orElseThrow().getName())
                .isEqualTo("Nhóm Phoenix Updated");

        mockMvc.perform(post("/faculty/groups/{id}/edit", groupId)
                        .param("name", "Should not save")
                        .param("leaderId", leaderId.toString())
                        .param("status", "ACTIVE")
                        .with(user(facultyHeadReadOnlyPrincipal()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void directoryPaginatesAndKeepsTheCurrentQueryState() throws Exception {
        RegistrationPeriodEntity period = registrationPeriodRepository.findAll().get(0);
        UserEntity leader = userRepository.findByLoginIdentifier("24110000").orElseThrow();
        for (int index = 0; index < 6; index++) {
            studentGroupRepository.save(new StudentGroupEntity(
                    "Pagination Group " + index, period, leader, leader));
        }
        studentGroupRepository.flush();

        mockMvc.perform(get("/faculty/groups")
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", "Pagination")
                        .param("sort", "group")
                        .param("direction", "asc")
                        .with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pagination Group 5")))
                .andExpect(content().string(not(containsString("Pagination Group 0"))))
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("search=pagination")));
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal() {
        return new DatabaseUserPrincipal(
                CNTT_HEAD, "", "PGS. TS. Nguyễn Văn Khang", "Faculty Head",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY_HEAD"),
                        new SimpleGrantedAuthority("GROUP_READ"),
                        new SimpleGrantedAuthority("GROUP_UPDATE")));
    }

    private static DatabaseUserPrincipal adminPrincipal() {
        return new DatabaseUserPrincipal(
                "admin@hcmute.edu.vn", "", "System Administrator", "Administrator",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("GROUP_READ"),
                        new SimpleGrantedAuthority("GROUP_UPDATE")));
    }

    private static DatabaseUserPrincipal userWithoutGroupRead() {
        return new DatabaseUserPrincipal(
                "lecturer@hcmute.edu.vn", "", "Lecturer", "Lecturer",
                List.of(new SimpleGrantedAuthority("ROLE_LECTURER")));
    }

    private static DatabaseUserPrincipal facultyHeadReadOnlyPrincipal() {
        return new DatabaseUserPrincipal(
                CNTT_HEAD, "", "PGS. TS. Nguyễn Văn Khang", "Faculty Head",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY_HEAD"),
                        new SimpleGrantedAuthority("GROUP_READ")));
    }

    private Long groupId(String name) {
        return studentGroupRepository.findAll().stream()
                .filter(group -> name.equals(group.getName()))
                .map(StudentGroupEntity::getId)
                .findFirst()
                .orElseThrow();
    }
}
