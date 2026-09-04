package com.hcmute.topicmanagement.web.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.hcmute.topicmanagement.service.RoleManagementService;
import com.hcmute.topicmanagement.service.RoleManagementService.RolePermissionData;
import com.hcmute.topicmanagement.web.form.PermissionAssignmentForm;

@Controller
@RequestMapping("/admin/roles")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    public RoleManagementController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public String list(@RequestParam(name = "roleId", required = false) Long roleId, Model model) {
        List<RoleManagementService.RoleSummary> roles = roleManagementService.listRoles();
        RolePermissionData selectedRole = selectRole(roles, roleId);
        PermissionAssignmentForm form = new PermissionAssignmentForm();
        if (selectedRole != null) {
            form.setPermissionIds(selectedRole.getPermissionIds());
        }
        return renderRoleWorkspace(model, roles, selectedRole, form);
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE') and hasAuthority('PERMISSION_ASSIGN')")
    public String permissionForm(@PathVariable Long id, Model model) {
        roleManagementService.getRoleForPermissions(id);
        return "redirect:/admin/roles?roleId=" + id;
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE') and hasAuthority('PERMISSION_ASSIGN')")
    public String updatePermissions(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") PermissionAssignmentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        RolePermissionData role = roleManagementService.getRoleForPermissions(id);
        if (bindingResult.hasErrors()) {
            return renderRoleWorkspace(model, roleManagementService.listRoles(), role, form);
        }

        try {
            roleManagementService.updateRolePermissions(id, form.getPermissionIds());
            redirectAttributes.addFlashAttribute("successMessage", "Role permissions updated successfully.");
            return "redirect:/admin/roles?roleId=" + id;
        } catch (RoleManagementService.RoleValidationException exception) {
            bindingResult.reject("permissions.invalid", exception.getMessage());
            return renderRoleWorkspace(model, roleManagementService.listRoles(), role, form);
        }
    }

    @ExceptionHandler(RoleManagementService.RoleValidationException.class)
    public String roleCannotBeManaged(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "This role is system-managed and cannot be changed here.");
        return "redirect:/admin/roles";
    }

    private String renderRoleWorkspace(
            Model model,
            List<RoleManagementService.RoleSummary> roles,
            RolePermissionData selectedRole,
            PermissionAssignmentForm form) {
        model.addAttribute("pageTitle", "Roles & access controls");
        model.addAttribute("roles", roles);
        model.addAttribute("selectedRole", selectedRole);
        model.addAttribute("form", form);
        model.addAttribute("permissionGroups", roleManagementService.getPermissionGroups());
        return "admin/roles";
    }

    private RolePermissionData selectRole(List<RoleManagementService.RoleSummary> roles, Long roleId) {
        if (roles.isEmpty()) {
            return null;
        }

        Long selectedId = roleId == null ? roles.get(0).getId() : roleId;
        boolean visibleRole = roles.stream().anyMatch(role -> role.getId().equals(selectedId));
        if (!visibleRole) {
            throw new RoleManagementService.RoleValidationException(
                    "Only visible system roles can be managed here.");
        }
        return roleManagementService.getRoleForPermissions(selectedId);
    }
}
