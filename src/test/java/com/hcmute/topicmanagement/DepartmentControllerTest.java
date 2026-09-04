package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void administratorCanViewAndCreateDepartment() throws Exception {
        String suffix = suffix();
        mockMvc.perform(get("/admin/departments").with(user(administrator(true))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/departments"))
                .andExpect(content().string(containsString("Department management")));

        mockMvc.perform(post("/admin/departments")
                        .with(user(administrator(true)))
                        .with(csrf())
                        .param("code", "cntt-" + suffix)
                        .param("name", "Information Technology " + suffix))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments"))
                .andExpect(flash().attribute("successMessage", "Department created successfully."));

        DepartmentEntity department = departmentRepository
                .findByCodeIgnoreCase("CNTT-" + suffix)
                .orElseThrow();
        assertEquals("Information Technology " + suffix, department.getName());
        assertTrue(department.isActive());
    }

    @Test
    void administratorWithManagePermissionCanViewDepartments() throws Exception {
        mockMvc.perform(get("/admin/departments").with(user(administrator(true))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/departments"));
    }

    @Test
    void administratorCanSearchAndSortDepartments() throws Exception {
        String suffix = suffix();
        departmentRepository.saveAndFlush(
                new DepartmentEntity("ZZZ-" + suffix, "Zulu Department " + suffix));
        departmentRepository.saveAndFlush(
                new DepartmentEntity("AAA-" + suffix, "Alpha Department " + suffix));

        mockMvc.perform(get("/admin/departments")
                        .with(user(administrator(true)))
                        .param("search", "Alpha Department " + suffix))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alpha Department " + suffix)))
                .andExpect(content().string(not(containsString("Zulu Department " + suffix))));

        MvcResult sortedResult = mockMvc.perform(get("/admin/departments")
                        .with(user(administrator(true)))
                        .param("search", suffix)
                        .param("sort", "code")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sort=code")))
                .andReturn();

        String response = sortedResult.getResponse().getContentAsString();
        assertTrue(response.indexOf("AAA-" + suffix) < response.indexOf("ZZZ-" + suffix));
    }

    @Test
    void administratorCanPaginateDepartmentsWhilePreservingDirectoryQuery() throws Exception {
        String suffix = suffix();
        for (int index = 0; index < 12; index++) {
            departmentRepository.saveAndFlush(new DepartmentEntity(
                    String.format("PAGE-%02d-%s", index, suffix),
                    "Pagination Department " + index + " " + suffix));
        }

        mockMvc.perform(get("/admin/departments")
                        .with(user(administrator(true)))
                        .param("page", "1")
                        .param("size", "5")
                        .param("search", suffix)
                        .param("sort", "code")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Showing")))
                .andExpect(content().string(containsString("PAGE-05-" + suffix)))
                .andExpect(content().string(not(containsString("PAGE-00-" + suffix))))
                .andExpect(content().string(containsString("size=5")))
                .andExpect(content().string(containsString("sort=code")))
                .andExpect(content().string(containsString("direction=asc")));
    }

    @Test
    void invalidDepartmentInputIsRejected() throws Exception {
        mockMvc.perform(post("/admin/departments")
                        .with(user(administrator(true)))
                        .with(csrf())
                        .param("code", "bad code")
                        .param("name", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/departments"))
                .andExpect(content().string(containsString("Department code may contain letters")))
                .andExpect(content().string(containsString("Department name is required.")));
    }

    @Test
    void duplicateCodeAndNameAreRejectedCaseInsensitively() throws Exception {
        String suffix = suffix();
        departmentRepository.saveAndFlush(new DepartmentEntity("DUP-" + suffix, "Department " + suffix));

        mockMvc.perform(post("/admin/departments")
                        .with(user(administrator(true)))
                        .with(csrf())
                        .param("code", "dup-" + suffix)
                        .param("name", "Another name " + suffix))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Department code is already in use."));

        mockMvc.perform(post("/admin/departments")
                        .with(user(administrator(true)))
                        .with(csrf())
                        .param("code", "OTHER-" + suffix)
                        .param("name", "department " + suffix))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Department name is already in use."));
    }

    @Test
    void administratorCanUpdateAndDeactivateDepartmentWithoutBreakingTopicReference() throws Exception {
        String suffix = suffix();
        UserEntity lecturer = userRepository.saveAndFlush(
                new UserEntity("lecturer-department-" + suffix, "Lecturer " + suffix, "test-password-hash"));
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        RegistrationPeriodEntity period = registrationPeriodRepository.saveAndFlush(new RegistrationPeriodEntity(
                "Period " + suffix, PeriodType.COURSE, start, end, start, end));
        DepartmentEntity department = departmentRepository.saveAndFlush(
                new DepartmentEntity("OLD-" + suffix, "Old Department " + suffix));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
                period, department, lecturer, "Topic " + suffix, "Description"));

        mockMvc.perform(post("/admin/departments/{id}/edit", department.getId())
                        .with(user(administrator(true)))
                        .with(csrf())
                        .param("code", "NEW-" + suffix)
                        .param("name", "New Department " + suffix))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Department updated successfully."));

        mockMvc.perform(post("/admin/departments/{id}/status", department.getId())
                        .with(user(administrator(true)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "Department deactivated successfully."));

        DepartmentEntity updated = departmentRepository.findById(department.getId()).orElseThrow();
        assertEquals("NEW-" + suffix, updated.getCode());
        assertEquals("New Department " + suffix, updated.getName());
        assertFalse(updated.isActive());
        assertNotNull(topicRepository.findById(topic.getId()).orElse(null));
        assertTrue(topicRepository.existsByDepartment_Id(department.getId()));

        mockMvc.perform(post("/admin/departments/{id}/delete", department.getId())
                        .with(user(administrator(true)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "errorMessage", "This department cannot be deleted because it is referenced by existing topics."));
    }

    @Test
    void administratorCanDeleteUnusedDepartment() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = departmentRepository.saveAndFlush(
                new DepartmentEntity("DELETE-" + suffix, "Delete Department " + suffix));

        mockMvc.perform(post("/admin/departments/{id}/delete", department.getId())
                        .with(user(administrator(true)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments"))
                .andExpect(flash().attribute("successMessage", "Department deleted successfully."));

        assertTrue(departmentRepository.findById(department.getId()).isEmpty());
    }

    @Test
    void departmentWithAssignedUsersCannotBeDeleted() throws Exception {
        String suffix = suffix();
        DepartmentEntity department = departmentRepository.saveAndFlush(
                new DepartmentEntity("USED-" + suffix, "Used Department " + suffix));
        UserEntity member = new UserEntity(
                "department-member-" + suffix,
                "Department Member " + suffix,
                "test-password-hash");
        member.setEmailOrCode("department-member-" + suffix.toLowerCase() + "@lecturer.hcmute.edu.vn");
        member.setDepartment(department);
        userRepository.saveAndFlush(member);

        mockMvc.perform(post("/admin/departments/{id}/delete", department.getId())
                        .with(user(administrator(true)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/departments"))
                .andExpect(flash().attribute(
                        "errorMessage", "This department cannot be deleted because it still has assigned users."));

        assertTrue(departmentRepository.findById(department.getId()).isPresent());
    }

    @Test
    void facultyHeadCanOnlyViewTheirDepartment() throws Exception {
        mockMvc.perform(get("/faculty/departments").with(user(facultyHead(false))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/my-department"));

        mockMvc.perform(post("/faculty/departments")
                        .with(user(facultyHead(false)))
                        .with(csrf())
                        .param("code", "NO-PERM-" + suffix())
                        .param("name", "No permission"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/admin/departments").with(user(facultyHead(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyHeadOnlySeesMembersFromTheirOwnDepartment() throws Exception {
        String suffix = suffix();
        DepartmentEntity ownDepartment = departmentRepository.saveAndFlush(
                new DepartmentEntity("OWN-" + suffix, "Own Department " + suffix));
        DepartmentEntity otherDepartment = departmentRepository.saveAndFlush(
                new DepartmentEntity("OTHER-" + suffix, "Other Department " + suffix));

        String headEmail = "head." + suffix.toLowerCase() + "@lecturer.hcmute.edu.vn";
        UserEntity head = new UserEntity("head-" + suffix, "Head " + suffix, "test-password-hash");
        head.setEmailOrCode(headEmail);
        head.setDepartment(ownDepartment);
        userRepository.saveAndFlush(head);

        UserEntity ownMember = new UserEntity("own-member-" + suffix, "Own Member " + suffix, "test-password-hash");
        ownMember.setEmailOrCode("own-member-" + suffix.toLowerCase() + "@lecturer.hcmute.edu.vn");
        ownMember.setDepartment(ownDepartment);
        userRepository.saveAndFlush(ownMember);

        UserEntity otherMember = new UserEntity("other-member-" + suffix, "Other Member " + suffix, "test-password-hash");
        otherMember.setEmailOrCode("other-member-" + suffix.toLowerCase() + "@lecturer.hcmute.edu.vn");
        otherMember.setDepartment(otherDepartment);
        userRepository.saveAndFlush(otherMember);

        mockMvc.perform(get("/faculty/departments").with(user(facultyHead(headEmail))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/my-department"))
                .andExpect(content().string(containsString("Own Department " + suffix)))
                .andExpect(content().string(containsString("Own Member " + suffix)))
                .andExpect(content().string(not(containsString("Other Member " + suffix))));
    }

    @Test
    void departmentMutationsRequireCsrf() throws Exception {
        String code = "NO-CSRF-" + suffix();
        mockMvc.perform(post("/admin/departments")
                        .with(user(administrator(true)))
                        .param("code", code)
                        .param("name", "No CSRF"))
                .andExpect(status().isForbidden());

        assertTrue(departmentRepository.findByCodeIgnoreCase(code).isEmpty());
    }

    private static DatabaseUserPrincipal facultyHead(boolean manageDepartments) {
        return facultyHead("faculty.head.test", manageDepartments);
    }

    private static DatabaseUserPrincipal facultyHead(String email) {
        return facultyHead(email, false);
    }

    private static DatabaseUserPrincipal facultyHead(String email, boolean manageDepartments) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_FACULTY_HEAD"));
        if (manageDepartments) {
            authorities.add(new SimpleGrantedAuthority("DEPARTMENT_MANAGE"));
        }
        return new DatabaseUserPrincipal(
                email, "", "Faculty Head Test", "Faculty Head", authorities);
    }

    private static DatabaseUserPrincipal administrator(boolean manageDepartments) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (manageDepartments) {
            authorities.add(new SimpleGrantedAuthority("DEPARTMENT_MANAGE"));
        }
        return new DatabaseUserPrincipal(
                "admin@hcmute.edu.vn", "", "System Administrator", "Administrator", authorities);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
