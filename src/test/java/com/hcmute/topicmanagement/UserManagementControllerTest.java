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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanOpenUserDirectoryWithoutLeakingPasswordHash() throws Exception {
        saveUser("directory-user", "Directory User", "STUDENT", "Password123");

        mockMvc.perform(get("/admin/users").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("User management")))
                .andExpect(content().string(containsString("Directory User")))
                .andExpect(content().string(not(containsString(">My department</span>"))))
                .andExpect(content().string(not(containsString("Password123"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void sidebarUserSectionsFilterStudentsAndLecturers() throws Exception {
        saveUser("student-filter", "Student Filter", "STUDENT", "Password123");
        saveUser("lecturer-filter", "Lecturer Filter", "LECTURER", "Password123");
        saveUser("faculty-head-filter", "Faculty Head Filter", "FACULTY_HEAD", "Password123");

        mockMvc.perform(get("/admin/users?role=STUDENT").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Student Filter")))
                .andExpect(content().string(not(containsString("Lecturer Filter"))))
                .andExpect(content().string(containsString("Manage students")));

        mockMvc.perform(get("/admin/users?role=LECTURER")
                        .with(user(admin("USER_READ", "USER_CREATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lecturer Filter")))
                .andExpect(content().string(containsString("Faculty Head Filter")))
                .andExpect(content().string(not(containsString("Student Filter"))))
                .andExpect(content().string(containsString("Manage lecturers")))
                .andExpect(content().string(containsString("data-modal-target=\"create-user-modal\"")))
                .andExpect(content().string(containsString("Add lecturer")))
                .andExpect(content().string(containsString("Email (Login identifier)")))
                .andExpect(content().string(not(containsString("data-student-code"))))
                .andExpect(content().string(not(containsString("Student profile"))))
                .andExpect(content().string(not(containsString("Use at least 8 characters."))));
    }

    @Test
    void dedicatedStudentAndLecturerRoutesRenderSeparatePages() throws Exception {
        saveUser("student-route", "Student Route", "STUDENT", "Password123");
        saveUser("lecturer-route", "Lecturer Route", "LECTURER", "Password123");

        mockMvc.perform(get("/admin/students").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/students"))
                .andExpect(content().string(containsString("Student Route")))
                .andExpect(content().string(not(containsString("Lecturer Route"))));

        mockMvc.perform(get("/admin/lecturers").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/lecturers"))
                .andExpect(content().string(containsString("Lecturer Route")))
                .andExpect(content().string(not(containsString("Student Route"))))
                .andExpect(content().string(not(containsString("Use at least 8 characters."))))
                .andExpect(content().string(not(containsString(">MSSV<"))));
    }

    @Test
    void lecturerDirectoryCanSortByDepartment() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        DepartmentEntity firstDepartment = departmentRepository.saveAndFlush(
                new DepartmentEntity("AAA-SORT-" + suffix, "AAA Sort Department " + suffix));
        DepartmentEntity secondDepartment = departmentRepository.saveAndFlush(
                new DepartmentEntity("ZZZ-SORT-" + suffix, "ZZZ Sort Department " + suffix));
        UserEntity firstLecturer = saveUser("department-sort-first-" + suffix,
                "Department Sort First " + suffix, "LECTURER", "Password123");
        firstLecturer.setDepartment(secondDepartment);
        userRepository.saveAndFlush(firstLecturer);
        UserEntity secondLecturer = saveUser("department-sort-second-" + suffix,
                "Department Sort Second " + suffix, "LECTURER", "Password123");
        secondLecturer.setDepartment(firstDepartment);
        userRepository.saveAndFlush(secondLecturer);

        String html = mockMvc.perform(get("/admin/lecturers")
                        .with(user(admin("USER_READ")))
                        .param("size", "100")
                        .param("sort", "department")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sort=department")))
                .andExpect(content().string(containsString("direction=asc")))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(
                html.indexOf("Department Sort Second " + suffix) < html.indexOf("Department Sort First " + suffix));
    }

    @Test
    void studentAndLecturerDirectoriesExposeDropdownActionsAndDeleteConfirmation() throws Exception {
        UserEntity student = saveUser("student-actions", "Student Actions", "STUDENT", "Password123");
        UserEntity lecturer = saveUser("lecturer-actions", "Lecturer Actions", "LECTURER", "Password123");
        DatabaseUserPrincipal administrator = admin("USER_READ", "USER_UPDATE", "USER_ROLE_ASSIGN", "USER_LOCK",
                "USER_DELETE");

        mockMvc.perform(get("/admin/students").with(user(administrator)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "data-dropdown-toggle=\"user-actions-menu-" + student.getId() + "\"")))
                .andExpect(content().string(containsString(
                        "data-modal-target=\"delete-user-modal-" + student.getId() + "\"")))
                .andExpect(content().string(containsString("Delete account?")))
                .andExpect(content().string(containsString(
                        "action=\"/admin/students/" + student.getId() + "/delete\"")));

        mockMvc.perform(get("/admin/lecturers").with(user(administrator)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "data-dropdown-toggle=\"user-actions-menu-" + lecturer.getId() + "\"")))
                .andExpect(content().string(containsString(
                        "data-modal-target=\"delete-user-modal-" + lecturer.getId() + "\"")))
                .andExpect(content().string(containsString("Delete account?")))
                .andExpect(content().string(containsString(
                        "action=\"/admin/lecturers/" + lecturer.getId() + "/delete\"")));
    }

    @Test
    void adminCanDeleteStudentAndLecturerAccountsThroughTheirDirectories() throws Exception {
        UserEntity student = saveUser("student-delete", "Student Delete", "STUDENT", "Password123");
        UserEntity lecturer = saveUser("lecturer-delete", "Lecturer Delete", "LECTURER", "Password123");

        mockMvc.perform(post("/admin/students/" + student.getId() + "/delete")
                        .with(user(admin("USER_DELETE")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students"))
                .andExpect(flash().attribute("successMessage", "Student account deleted successfully."));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(student.getId()).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.findByUser_Id(student.getId()).isEmpty());

        mockMvc.perform(post("/admin/lecturers/" + lecturer.getId() + "/delete")
                        .with(user(admin("USER_DELETE")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lecturers"))
                .andExpect(flash().attribute("successMessage", "Lecturer account deleted successfully."));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(lecturer.getId()).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.findByUser_Id(lecturer.getId()).isEmpty());
    }

    @Test
    void deleteRequiresPermissionAndCannotDeleteCurrentAccount() throws Exception {
        UserEntity protectedUser = saveUser("delete-permission", "Delete Permission", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/students/" + protectedUser.getId() + "/delete")
                        .with(user(admin("USER_READ")))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        UserEntity currentUser = saveUser("delete-current", "Delete Current", "STUDENT", "Password123");
        mockMvc.perform(post("/admin/students/" + currentUser.getId() + "/delete")
                        .with(user(adminAs("delete-current", "USER_DELETE")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students"))
                .andExpect(flash().attribute("errorMessage", "You cannot delete your own account."));

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(currentUser.getId()).isPresent());
    }

    @Test
    void lecturerEditModalHidesLoginPasswordAndRoleControls() throws Exception {
        saveUser("lecturer-edit", "Lecturer Edit", "LECTURER", "Password123");

        mockMvc.perform(get("/admin/lecturers").with(user(admin("USER_READ", "USER_UPDATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Login identifier is immutable after account creation."))))
                .andExpect(content().string(not(containsString("New password (optional)"))))
                .andExpect(content().string(not(containsString("System roles"))))
                .andExpect(content().string(containsString("Confirm password")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")));
    }

    @Test
    void settingPasswordRequiresMatchingConfirmation() throws Exception {
        UserEntity student = saveUser("password-confirmation-student", "Password Confirmation Student", "STUDENT",
                "OldPass123");

        mockMvc.perform(post("/admin/students/" + student.getId() + "/password")
                        .with(user(admin("USER_UPDATE")))
                        .with(csrf())
                        .param("password", "NewPass123")
                        .param("confirmPassword", "Different123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students"))
                .andExpect(flash().attribute("errorMessage",
                        "Password must be between 8 and 72 characters, and both passwords must match."));

        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("OldPass123",
                userRepository.findById(student.getId()).orElseThrow().getPasswordHash()));

        UserEntity lecturer = saveUser("password-confirmation-lecturer", "Password Confirmation Lecturer", "LECTURER",
                "OldPass123");

        mockMvc.perform(post("/admin/lecturers/" + lecturer.getId() + "/password")
                        .with(user(admin("USER_UPDATE")))
                        .with(csrf())
                        .param("password", "NewPass123")
                        .param("confirmPassword", "NewPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lecturers"))
                .andExpect(flash().attribute("successMessage", "Lecturer password set successfully."));

        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("NewPass123",
                userRepository.findById(lecturer.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void userDirectoryRendersReusableCreateAndEditModals() throws Exception {
        UserEntity existing = saveUser("modal-user", "Modal User", "STUDENT", "Password123");

        mockMvc.perform(get("/admin/users").with(user(admin(
                        "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-modal-toggle=\"create-user-modal\"")))
                .andExpect(content().string(containsString("id=\"create-user-modal\"")))
                .andExpect(content().string(containsString(
                        "data-modal-toggle=\"edit-user-modal-" + existing.getId() + "\"")))
                .andExpect(content().string(containsString(
                        "id=\"edit-user-modal-" + existing.getId() + "\"")))
                .andExpect(content().string(containsString("Modal User")));
    }

    @Test
    void adminCanOpenCreateAndEditScreens() throws Exception {
        UserEntity existing = saveUser("form-user", "Form User", "STUDENT", "Password123");

        mockMvc.perform(get("/admin/users/new")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("Create account")))
                .andExpect(content().string(containsString("Student role is assigned automatically")))
                .andExpect(content().string(not(containsString("Account role"))))
                .andExpect(content().string(not(containsString("Student profile"))))
                .andExpect(content().string(not(containsString("data-role-code=\"FACULTY_HEAD\""))));

        mockMvc.perform(get("/admin/users/" + existing.getId() + "/edit")
                        .with(user(admin("USER_UPDATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("Form User")))
                .andExpect(content().string(containsString("Save changes")));
    }

    @Test
    void administratorAccountsAreHiddenAndCannotBeEditedOrLocked() throws Exception {
        UserEntity protectedAdmin = saveUser("protected-admin", "Protected Admin", "ADMIN", "Password123");
        saveUser("visible-user", "Visible User", "STUDENT", "Password123");

        mockMvc.perform(get("/admin/users").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visible User")))
                .andExpect(content().string(not(containsString("Protected Admin"))))
                .andExpect(content().string(not(containsString("protected-admin"))));

        mockMvc.perform(get("/admin/users/" + protectedAdmin.getId() + "/edit")
                        .with(user(admin("USER_UPDATE", "USER_ROLE_ASSIGN"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute(
                        "errorMessage", "Administrator accounts are managed outside this screen."));

        mockMvc.perform(post("/admin/users/" + protectedAdmin.getId() + "/status")
                        .with(user(admin("USER_LOCK")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute(
                        "errorMessage", "Administrator accounts are managed outside this screen."));
        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(protectedAdmin.getId()).orElseThrow().isActive());
    }

    @Test
    void nonAdminCannotOpenUserDirectory() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(new DatabaseUserPrincipal(
                "student", "", "Student", "Student", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentAccountCreationRequiresUserCreatePermissionAndCsrf() throws Exception {
        mockMvc.perform(get("/admin/users/new")
                        .with(user(admin("USER_CREATE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE")))
                        .param("loginIdentifier", "csrf-user")
                        .param("fullName", "CSRF User")
                        .param("password", "StrongPass123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateStudentAccountWithGeneratedEmailAndUniqueMssv() throws Exception {
        RoleEntity student = role("STUDENT", "Student");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "created-user")
                        .param("fullName", "Created User")
                        .param("emailOrCode", "ignored-value-is-not-used")
                        .param("password", "StrongPass123")
                        .param("roleIds", student.getId().toString())
                        .param("studentCode", "24110001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity created = userRepository.findByLoginIdentifier("24110001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("StrongPass123", created.getPasswordHash()));
        org.junit.jupiter.api.Assertions.assertEquals("24110001@student.hcmute.edu.vn", created.getEmailOrCode());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(
                created.getId(), student.getId()));
    }

    @Test
    void dedicatedStudentCreateUsesMssvAsLoginAssignsStudentAndLeavesPasswordUnset() throws Exception {
        RoleEntity student = role("STUDENT", "Student");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("accountType", "STUDENT")
                        .param("fullName", "Dedicated Student")
                        .param("studentCode", "24110004")
                        .param("roleIds", role("LECTURER", "Lecturer").getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity created = userRepository.findByLoginIdentifier("24110004").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(created.getPasswordHash().isBlank());
        org.junit.jupiter.api.Assertions.assertEquals("24110004@student.hcmute.edu.vn", created.getEmailOrCode());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(
                created.getId(), student.getId()));
    }

    @Test
    void studentCreateModalDoesNotExposeRoleOrPasswordFields() throws Exception {
        mockMvc.perform(get("/admin/users?role=STUDENT")
                .with(user(admin("USER_READ", "USER_CREATE"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Student code (MSSV)")))
                .andExpect(content().string(containsString("data-student-code")))
                .andExpect(content().string(containsString("Student role is assigned automatically")))
                .andExpect(content().string(not(containsString("Login identifier"))))
                .andExpect(content().string(not(containsString("Use at least 8 characters."))))
                .andExpect(content().string(not(containsString("Account role"))));
    }

    @Test
    void adminCanCreateLecturerWithManualEmail() throws Exception {
        RoleEntity lecturer = role("LECTURER", "Lecturer");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "created-lecturer")
                        .param("fullName", "Created Lecturer")
                        .param("emailOrCode", "created.lecturer@hcmute.edu.vn")
                        .param("password", "StrongPass123")
                        .param("roleIds", lecturer.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity created = userRepository.findByLoginIdentifier("created-lecturer").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("created.lecturer@hcmute.edu.vn", created.getEmailOrCode());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(
                created.getId(), lecturer.getId()));
    }

    @Test
    void dedicatedLecturerCreateAssignsLecturerWithoutShowingStudentFields() throws Exception {
        RoleEntity lecturer = role("LECTURER", "Lecturer");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("accountType", "LECTURER")
                        .param("loginIdentifier", "this-value-must-be-ignored")
                        .param("fullName", "Dedicated Lecturer")
                        .param("emailOrCode", "dedicated.lecturer@hcmute.edu.vn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity created = userRepository.findByLoginIdentifier("dedicated.lecturer@hcmute.edu.vn").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(created.getPasswordHash().isBlank());
        org.junit.jupiter.api.Assertions.assertEquals("dedicated.lecturer@hcmute.edu.vn", created.getEmailOrCode());
        org.junit.jupiter.api.Assertions.assertTrue(userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(
                created.getId(), lecturer.getId()));
    }

    @Test
    void createCannotAssignFacultyHeadDirectly() throws Exception {
        RoleEntity facultyHead = role("FACULTY_HEAD", "Faculty Head");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "created-faculty-head")
                        .param("fullName", "Created Faculty Head")
                        .param("emailOrCode", "faculty.head@hcmute.edu.vn")
                        .param("password", "StrongPass123")
                        .param("roleIds", facultyHead.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("One or more selected roles are invalid or inactive.")));
    }

    @Test
    void editingStudentKeepsMssvAndGeneratedEmailEvenWhenPayloadTriesToChangeIt() throws Exception {
        RoleEntity student = role("STUDENT", "Student");
        UserEntity existing = saveUser("24110003", "Before Update", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/users/" + existing.getId() + "/edit")
                        .with(user(admin("USER_UPDATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "24110003")
                        .param("fullName", "After Update")
                        .param("emailOrCode", "attacker@example.com")
                        .param("password", "")
                        .param("roleIds", student.getId().toString())
                        .param("studentCode", "99999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity updated = userRepository.findById(existing.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("After Update", updated.getFullName());
        org.junit.jupiter.api.Assertions.assertEquals("24110003@student.hcmute.edu.vn", updated.getEmailOrCode());
    }

    @Test
    void createdAccountCanSignInWithEmailThroughTheLoginFlow() throws Exception {
        saveUser("login-user", "Login User", "STUDENT", "StrongPass123");

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "login-user@hcmute.local")
                        .param("password", "StrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "login-user")
                        .param("password", "StrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void createRejectsDuplicateLoginIdentifier() throws Exception {
        saveUser("duplicate-user", "Existing User", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", " DUPLICATE-USER ")
                        .param("fullName", "Another User")
                        .param("password", "StrongPass123")
                        .param("roleIds", role("LECTURER", "Lecturer").getId().toString())
                        .param("emailOrCode", "another@hcmute.edu.vn"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("Login identifier is already in use.")));
    }

    @Test
    void updatePreservesPasswordWhenNewPasswordIsBlank() throws Exception {
        UserEntity user = saveUser("update-user", "Before Update", "STUDENT", "OldPass123");
        RoleEntity role = role("LECTURER", "Lecturer");

        mockMvc.perform(post("/admin/users/" + user.getId() + "/edit")
                        .with(user(admin("USER_UPDATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "update-user")
                        .param("fullName", "After Update")
                        .param("emailOrCode", "after@hcmute.local")
                        .param("password", "")
                        .param("roleIds", role.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity updated = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("After Update", updated.getFullName());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("OldPass123", updated.getPasswordHash()));
        org.junit.jupiter.api.Assertions.assertTrue(
                userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(user.getId(), role.getId()));
    }

    @Test
    void lockAndUnlockAccountRoundTrip() throws Exception {
        UserEntity user = saveUser("lock-user", "Lock User", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/users/" + user.getId() + "/status")
                        .with(user(admin("USER_LOCK")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "User account locked successfully."));
        org.junit.jupiter.api.Assertions.assertFalse(userRepository.findById(user.getId()).orElseThrow().isActive());

        mockMvc.perform(post("/admin/users/" + user.getId() + "/status")
                        .with(user(admin("USER_LOCK")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "User account unlocked successfully."));
        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(user.getId()).orElseThrow().isActive());
    }

    @Test
    void cannotChangeOwnAccountStatus() throws Exception {
        UserEntity user = saveUser("session-user", "Session User", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/users/" + user.getId() + "/status")
                        .with(user(adminAs("session-user", "USER_LOCK")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "You cannot lock or unlock your own account."));
        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(user.getId()).orElseThrow().isActive());
    }

    @Test
    void createRejectsDuplicateStudentCode() throws Exception {
        RoleEntity student = role("STUDENT", "Student");
        UserEntity existing = saveUser("24110002", "Existing Student", "STUDENT", "Password123");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "duplicate-student-code")
                        .param("fullName", "Duplicate Student Code")
                        .param("password", "StrongPass123")
                        .param("roleIds", student.getId().toString())
                        .param("studentCode", "24110002"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("Student code is already in use as a login identifier.")));
    }

    private UserEntity saveUser(String loginIdentifier, String fullName, String roleCode, String password) {
        RoleEntity role = role(roleCode, roleCode);
        UserEntity user = new UserEntity(loginIdentifier, fullName, passwordEncoder.encode(password));
        user.setEmailOrCode(loginIdentifier + "@hcmute.local");
        UserEntity saved = userRepository.saveAndFlush(user);
        userRoleRepository.saveAndFlush(new UserRoleEntity(saved, role));
        return saved;
    }

    private RoleEntity role(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> roleRepository.saveAndFlush(
                new RoleEntity(code, name, name + " role.")));
    }

    private static DatabaseUserPrincipal admin(String... permissions) {
        return adminAs("admin", permissions);
    }

    private static DatabaseUserPrincipal adminAs(String username, String... permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new DatabaseUserPrincipal(username, "", "System Administrator", "Administrator", authorities);
    }
}
