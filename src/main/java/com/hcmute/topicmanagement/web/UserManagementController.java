package com.hcmute.topicmanagement.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.UserManagementService;
import com.hcmute.topicmanagement.service.UserManagementService.UserEditorData;
import com.hcmute.topicmanagement.service.UserManagementService.UserSummary;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public String list(Model model) {
        List<UserSummary> users = userManagementService.listUsers();
        users.forEach(user -> model.addAttribute(
                "editForm" + user.getId(), toForm(userManagementService.getUser(user.getId()))));
        model.addAttribute("pageTitle", "User management");
        model.addAttribute("users", users);
        model.addAttribute("activeUserCount", users.stream().filter(UserSummary::isActive).count());
        model.addAttribute("lockedUserCount", users.stream().filter(user -> !user.isActive()).count());
        model.addAttribute("createForm", new UserForm());
        model.addAttribute("roles", userManagementService.listAssignableRoles());
        return "admin/users";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String createForm(Model model) {
        UserForm form = new UserForm();
        return renderForm(model, form, false, null);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String create(
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validatePassword(form, bindingResult, true);
        if (bindingResult.hasErrors()) {
            return renderForm(model, form, false, null);
        }

        try {
            userManagementService.createUser(
                    form.getLoginIdentifier(),
                    form.getFullName(),
                    form.getEmailOrCode(),
                    form.getPassword(),
                    form.getRoleIds());
            redirectAttributes.addFlashAttribute("successMessage", "User account created successfully.");
            return "redirect:/admin/users";
        } catch (UserManagementService.UserValidationException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return renderForm(model, form, false, null);
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String editForm(@PathVariable Long id, Model model) {
        UserEditorData user = userManagementService.getUser(id);
        UserForm form = toForm(user);
        return renderForm(model, form, true, user);
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        UserEditorData user = userManagementService.getUser(id);
        validatePassword(form, bindingResult, false);
        if (bindingResult.hasErrors()) {
            return renderForm(model, form, true, user);
        }

        try {
            userManagementService.updateUser(
                    id,
                    form.getFullName(),
                    form.getEmailOrCode(),
                    form.getPassword(),
                    form.getRoleIds());
            redirectAttributes.addFlashAttribute("successMessage", "User account updated successfully.");
            return "redirect:/admin/users";
        } catch (UserManagementService.UserValidationException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return renderForm(model, form, true, user);
        }
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public String toggleStatus(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean active = userManagementService.toggleActive(id, authentication.getName());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                active ? "User account unlocked successfully." : "User account locked successfully.");
        return "redirect:/admin/users";
    }

    @ExceptionHandler(UserManagementService.UserValidationException.class)
    public String userCannotBeChanged(
            UserManagementService.UserValidationException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/admin/users";
    }

    @ExceptionHandler(UserManagementService.UserNotFoundException.class)
    public String userNotFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "User account was not found.");
        return "redirect:/admin/users";
    }

    private String renderForm(Model model, UserForm form, boolean editMode, UserEditorData user) {
        model.addAttribute("pageTitle", editMode ? "Edit user" : "Create user");
        model.addAttribute("form", form);
        model.addAttribute("roles", userManagementService.listAssignableRoles());
        model.addAttribute("editMode", editMode);
        model.addAttribute("user", user);
        model.addAttribute("formAction", editMode
                ? "/admin/users/" + user.getId() + "/edit"
                : "/admin/users");
        return "admin/user-form";
    }

    private static UserForm toForm(UserEditorData user) {
        UserForm form = new UserForm();
        form.setLoginIdentifier(user.getLoginIdentifier());
        form.setFullName(user.getFullName());
        form.setEmailOrCode(user.getEmailOrCode());
        form.setRoleIds(user.getRoleIds());
        return form;
    }

    private static void validatePassword(UserForm form, BindingResult bindingResult, boolean required) {
        String password = form.getPassword();
        if (required && !StringUtils.hasText(password)) {
            bindingResult.rejectValue("password", "password.required", "Password is required when creating an account.");
        } else if (StringUtils.hasText(password) && password.length() < 8) {
            bindingResult.rejectValue("password", "password.short", "Password must be at least 8 characters.");
        }
    }
}
