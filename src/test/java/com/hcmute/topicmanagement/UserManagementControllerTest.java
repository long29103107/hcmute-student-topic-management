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
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.RoleRepository;
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
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanOpenUserDirectoryWithoutLeakingPasswordHash() throws Exception {
        saveUser("directory-user", "Directory User", "STUDENT", "Password123");

        mockMvc.perform(get("/admin/users").with(user(admin("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("User management")))
                .andExpect(content().string(containsString("Directory User")))
                .andExpect(content().string(not(containsString("Password123"))))
                .andExpect(content().string(not(containsString("$2a$"))));
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
                .andExpect(content().string(containsString("Create account")));

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
    void userMutationRequiresRoleAssignmentPermissionAndCsrf() throws Exception {
        mockMvc.perform(get("/admin/users/new")
                        .with(user(admin("USER_CREATE"))))
                .andExpect(status().isForbidden());

        RoleEntity role = role("CSRF_ROLE", "CSRF role");
        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .param("loginIdentifier", "csrf-user")
                        .param("fullName", "CSRF User")
                        .param("password", "StrongPass123")
                        .param("roleIds", role.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUserWithBcryptPasswordAndRole() throws Exception {
        RoleEntity role = role("STUDENT", "Student");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "created-user")
                        .param("fullName", "Created User")
                        .param("emailOrCode", "created@hcmute.local")
                        .param("password", "StrongPass123")
                        .param("roleIds", role.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserEntity created = userRepository.findByLoginIdentifier("created-user").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("StrongPass123", created.getPasswordHash()));
        org.junit.jupiter.api.Assertions.assertTrue(
                userRoleRepository.existsByUser_IdAndRole_IdAndActiveTrue(created.getId(), role.getId()));
    }

    @Test
    void createdAccountCanSignInThroughTheLoginFlow() throws Exception {
        saveUser("login-user", "Login User", "STUDENT", "StrongPass123");

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "login-user")
                        .param("password", "StrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void createRejectsDuplicateLoginIdentifier() throws Exception {
        saveUser("duplicate-user", "Existing User", "STUDENT", "Password123");
        RoleEntity role = role("LECTURER", "Lecturer");

        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", " DUPLICATE-USER ")
                        .param("fullName", "Another User")
                        .param("password", "StrongPass123")
                        .param("roleIds", role.getId().toString()))
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
    void createRejectsMissingRole() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .with(user(admin("USER_CREATE", "USER_ROLE_ASSIGN")))
                        .with(csrf())
                        .param("loginIdentifier", "no-role-user")
                        .param("fullName", "No Role User")
                        .param("password", "StrongPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(content().string(containsString("Select at least one active system role.")));
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
