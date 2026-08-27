package com.hcmute.topicmanagement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
class RoleManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void adminCanOpenRoleDirectory() throws Exception {
        mockMvc.perform(get("/admin/roles").with(user(admin("ROLE_READ"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/roles"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Role permissions")));
    }

    @Test
    void adminCanOpenPermissionAssignmentForm() throws Exception {
        RoleEntity role = roleRepository.save(new RoleEntity(
                "FACULTY_HEAD", "Faculty Head", "Manages faculty workflows."));

        mockMvc.perform(get("/admin/roles/" + role.getId() + "/permissions")
                        .with(user(admin("ROLE_UPDATE", "PERMISSION_ASSIGN"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/role-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manage permissions")));
    }

    @Test
    void adminRoleIsHiddenFromRoleDirectory() throws Exception {
        saveAdminRole();

        mockMvc.perform(get("/admin/roles").with(user(admin("ROLE_READ"))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Reserved administrator system role."))));
    }

    @Test
    void adminRoleCannotOpenPermissionAssignmentForm() throws Exception {
        RoleEntity role = saveAdminRole();

        mockMvc.perform(get("/admin/roles/" + role.getId() + "/permissions")
                        .with(user(admin("ROLE_UPDATE", "PERMISSION_ASSIGN"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/admin/roles"));
    }

    private RoleEntity saveAdminRole() {
        return roleRepository.findByCode("ADMIN").orElseGet(() -> roleRepository.save(new RoleEntity(
                "ADMIN", "Administrator", "Reserved administrator system role.")));
    }

    private static DatabaseUserPrincipal admin(String... permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new DatabaseUserPrincipal("admin", "", "System Administrator", "Administrator", authorities);
    }
}
