package com.hcmute.topicmanagement.web;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.hcmute.topicmanagement.service.RoleManagementService;
import com.hcmute.topicmanagement.service.RoleManagementService.RolePermissionData;

@Controller
@RequestMapping("/admin/roles")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    public RoleManagementController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public String list(Model model) {
        model.addAttribute("pageTitle", "Role permissions");
        model.addAttribute("roles", roleManagementService.listRoles());
        return "admin/roles";
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE') and hasAuthority('PERMISSION_ASSIGN')")
    public String permissionForm(@PathVariable Long id, Model model) {
        RolePermissionData role = roleManagementService.getRoleForPermissions(id);
        PermissionAssignmentForm form = new PermissionAssignmentForm();
        form.setPermissionIds(role.getPermissionIds());
        return renderPermissionForm(model, form, role);
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
            return renderPermissionForm(model, form, role);
        }

        try {
            roleManagementService.updateRolePermissions(id, form.getPermissionIds());
            redirectAttributes.addFlashAttribute("successMessage", "Role permissions updated successfully.");
            return "redirect:/admin/roles/" + id + "/permissions";
        } catch (RoleManagementService.RoleValidationException exception) {
            bindingResult.reject("permissions.invalid", exception.getMessage());
            return renderPermissionForm(model, form, role);
        }
    }

    @ExceptionHandler(RoleManagementService.RoleValidationException.class)
    public String roleCannotBeManaged(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "This role is system-managed and cannot be changed here.");
        return "redirect:/admin/roles";
    }

    private String renderPermissionForm(Model model, PermissionAssignmentForm form, RolePermissionData role) {
        model.addAttribute("pageTitle", "Manage permissions");
        model.addAttribute("form", form);
        model.addAttribute("role", role);
        model.addAttribute("permissionGroups", roleManagementService.getPermissionGroups());
        return "admin/role-form";
    }
}
