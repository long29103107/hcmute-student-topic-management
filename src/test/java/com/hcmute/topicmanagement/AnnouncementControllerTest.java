package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.AnnouncementEntity;
import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.model.enums.AnnouncementStatus;
import com.hcmute.topicmanagement.repository.AnnouncementRepository;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void readerSsrShowsOnlyPublishedAnnouncementsInDepartmentScope() throws Exception {
        DepartmentEntity cntt = saveDepartment("CNTT");
        DepartmentEntity business = saveDepartment("BUS");
        UserEntity admin = saveUser("ADMIN", null);
        UserEntity student = saveUser("STUDENT", cntt);

        saveAnnouncement(admin, "School notice", AnnouncementScope.SCHOOL, null, AnnouncementStatus.PUBLISHED);
        saveAnnouncement(admin, "CNTT notice", AnnouncementScope.DEPARTMENT, cntt, AnnouncementStatus.PUBLISHED);
        saveAnnouncement(admin, "Business notice", AnnouncementScope.DEPARTMENT, business, AnnouncementStatus.PUBLISHED);
        saveAnnouncement(admin, "Draft notice", AnnouncementScope.SCHOOL, null, AnnouncementStatus.DRAFT);
        saveAnnouncement(admin, "Hidden notice", AnnouncementScope.SCHOOL, null, AnnouncementStatus.HIDDEN);

        mockMvc.perform(get("/announcements").with(user(principal(student, "STUDENT", false))))
                .andExpect(status().isOk())
                .andExpect(view().name("announcements/list"))
                .andExpect(content().string(containsString("School notice")))
                .andExpect(content().string(containsString("CNTT notice")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Business notice"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Draft notice"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Hidden notice"))));
    }

    @Test
    void managerSsrSupportsCreatePublishHideAndShowsConfirmationModal() throws Exception {
        DepartmentEntity cntt = saveDepartment("CNTT");
        UserEntity admin = saveUser("ADMIN", null);

        mockMvc.perform(post("/announcements/manage")
                        .with(user(principal(admin, "ADMIN", true)))
                        .with(csrf())
                        .param("title", "New notice")
                        .param("content", "New content")
                        .param("scope", "DEPARTMENT")
                        .param("departmentId", cntt.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/announcements/manage"));

        AnnouncementEntity created = announcementRepository.findAll().stream()
                .filter(item -> "New notice".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/announcements/manage")
                        .with(user(principal(admin, "ADMIN", true)))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("announcements/manage"))
                .andExpect(content().string(containsString("data-announcement-confirm-modal")))
                .andExpect(content().string(containsString("data-modal-target=\"create-announcement-modal\"")))
                .andExpect(content().string(containsString("Search announcements")))
                .andExpect(content().string(containsString("Publish")))
                .andExpect(content().string(containsString("value=\"SCHOOL\"")))
                .andExpect(content().string(containsString("School-wide")));

        mockMvc.perform(post("/announcements/manage/publish")
                        .with(user(principal(admin, "ADMIN", true)))
                        .with(csrf())
                        .param("announcementId", created.getId().toString()))
                .andExpect(status().is3xxRedirection());
        announcementRepository.flush();
        org.assertj.core.api.Assertions.assertThat(
                announcementRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(AnnouncementStatus.PUBLISHED);

        mockMvc.perform(post("/announcements/manage/hide")
                        .with(user(principal(admin, "ADMIN", true)))
                        .with(csrf())
                        .param("announcementId", created.getId().toString()))
                .andExpect(status().is3xxRedirection());
        announcementRepository.flush();
        org.assertj.core.api.Assertions.assertThat(
                announcementRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(AnnouncementStatus.HIDDEN);
    }

    @Test
    void facultyHeadManagementUiOnlyOffersDepartmentScope() throws Exception {
        DepartmentEntity cntt = saveDepartment("CNTT");
        UserEntity facultyHead = saveUser("FACULTY_HEAD", cntt);

        mockMvc.perform(get("/announcements/manage")
                        .with(user(principal(facultyHead, "FACULTY_HEAD", true)))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("announcements/manage"))
                .andExpect(content().string(containsString("value=\"DEPARTMENT\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"SCHOOL\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("School-wide"))))
                .andExpect(content().string(containsString("CNTT-")));
    }

    @Test
    void restReaderIsScopedAndStateChangingRequestsRequireCsrf() throws Exception {
        DepartmentEntity cntt = saveDepartment("CNTT");
        DepartmentEntity business = saveDepartment("BUS");
        UserEntity admin = saveUser("ADMIN", null);
        UserEntity student = saveUser("STUDENT", cntt);

        saveAnnouncement(admin, "School notice", AnnouncementScope.SCHOOL, null, AnnouncementStatus.PUBLISHED);
        saveAnnouncement(admin, "CNTT notice", AnnouncementScope.DEPARTMENT, cntt, AnnouncementStatus.PUBLISHED);
        saveAnnouncement(admin, "Business notice", AnnouncementScope.DEPARTMENT, business, AnnouncementStatus.PUBLISHED);

        mockMvc.perform(get("/api/announcements").with(user(principal(student, "STUDENT", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "School notice", "CNTT notice")));

        mockMvc.perform(post("/api/announcements/manage")
                        .with(user(principal(admin, "ADMIN", true)))
                        .contentType("application/json")
                        .content("""
                                {"title":"REST notice","content":"REST content","scope":"SCHOOL","departmentId":null}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/announcements/manage")
                        .with(user(principal(admin, "ADMIN", true)))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"title":"REST notice","content":"REST content","scope":"SCHOOL","departmentId":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("DRAFT"));
    }

    @Test
    void studentCannotOpenManagementAdapters() throws Exception {
        UserEntity student = saveUser("STUDENT", null);

        mockMvc.perform(get("/announcements/manage")
                        .with(user(principal(student, "STUDENT", false)))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forbidden"));

        mockMvc.perform(get("/api/announcements/manage")
                        .with(user(principal(student, "STUDENT", false))))
                .andExpect(status().isForbidden());
    }

    private DepartmentEntity saveDepartment(String code) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return departmentRepository.saveAndFlush(new DepartmentEntity(
                code + "-" + suffix, "Announcement " + code + " " + suffix));
    }

    private UserEntity saveUser(String roleCode, DepartmentEntity department) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        UserEntity user = new UserEntity(
                roleCode.toLowerCase() + "-" + suffix,
                "Announcement " + roleCode,
                "test-password-hash");
        user.setEmailOrCode(roleCode.toLowerCase() + "-" + suffix + "@example.test");
        user.setDepartment(department);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        UserRoleEntity assignment = new UserRoleEntity(user, role);
        user.getUserRoles().add(assignment);
        userRoleRepository.saveAndFlush(assignment);
        return user;
    }

    private AnnouncementEntity saveAnnouncement(
            UserEntity author,
            String title,
            AnnouncementScope scope,
            DepartmentEntity department,
            AnnouncementStatus status) {
        AnnouncementEntity announcement = new AnnouncementEntity(
                title, "Announcement content.", scope, department, author);
        announcement.setStatus(status);
        if (status == AnnouncementStatus.PUBLISHED || status == AnnouncementStatus.HIDDEN) {
            announcement.setPublishedAt(java.time.LocalDateTime.now());
        }
        return announcementRepository.saveAndFlush(announcement);
    }

    private DatabaseUserPrincipal principal(UserEntity user, String roleCode, boolean management) {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(
                List.of(new SimpleGrantedAuthority("ROLE_" + roleCode)));
        if (management) {
            authorities.add(new SimpleGrantedAuthority("ANNOUNCEMENT_MANAGE"));
        }
        return new DatabaseUserPrincipal(
                user.getEmailOrCode(),
                user.getPasswordHash(),
                user.getFullName(),
                roleCode,
                authorities);
    }
}
