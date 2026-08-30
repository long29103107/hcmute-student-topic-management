package com.hcmute.topicmanagement.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.UserManagementService;
import com.hcmute.topicmanagement.service.UserManagementService.UserEditorData;
import com.hcmute.topicmanagement.service.UserManagementService.UserSummary;

@Controller
@RequestMapping("/admin/lecturers")
public class LecturerManagementController {

    private final UserManagementService service;

    public LecturerManagementController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "account") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String search, Model model) {
        UserManagementService.UserDirectoryPage directory = service.listUsersPage("LECTURER", search, page, size, sort, direction);
        populateDirectory(model, directory, search);
        model.addAttribute("lecturerForm", new LecturerForm());
        model.addAttribute("createForm", model.getAttribute("lecturerForm"));
        model.addAttribute("createFormName", "lecturerForm");
        model.addAttribute("formAction", "/admin/lecturers");
        model.addAttribute("statusBasePath", "/admin/lecturers");
        model.addAttribute("editBasePath", "/admin/lecturers");
        model.addAttribute("directoryPath", "/admin/lecturers");
        return "admin/lecturers";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String create(@Valid @ModelAttribute("lecturerForm") LecturerForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(form.getEmailOrCode())) {
            bindingResult.rejectValue("emailOrCode", "email.required", "Email is required for a lecturer.");
        }
        if (bindingResult.hasErrors()) {
            populateDirectory(model, service.listUsersPage("LECTURER", "", 0, 20, "account", "asc"), "");
            model.addAttribute("lecturerForm", form);
            model.addAttribute("createForm", form);
            model.addAttribute("createFormName", "lecturerForm");
            model.addAttribute("formAction", "/admin/lecturers");
            model.addAttribute("statusBasePath", "/admin/lecturers");
            model.addAttribute("editBasePath", "/admin/lecturers");
            return "admin/lecturers";
        }
        try {
            service.createLecturer(form.getFullName(), form.getEmailOrCode(), form.getPhone(), form.getDateOfBirth());
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer account created successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public String toggleStatus(@PathVariable Long id, org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean active = service.toggleActive(id, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", active ? "Lecturer account unlocked successfully." : "Lecturer account locked successfully.");
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String delete(@PathVariable Long id, org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            service.deleteUser(id, authentication.getName(), "LECTURER");
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer account deleted successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String setPassword(@PathVariable Long id, @Valid @ModelAttribute("passwordForm") PasswordForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (StringUtils.hasText(form.getPassword()) && !form.hasMatchingPasswords()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Password must be between 8 and 72 characters, and both passwords must match.");
            return "redirect:/admin/lecturers";
        }
        try {
            service.setPassword(id, form.getPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer password set successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("editForm") LecturerForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the lecturer details.");
            return "redirect:/admin/lecturers";
        }
        try {
            form.setRoleIds(service.getUser(id).getRoleIds());
            service.updateUser(id, form.getFullName(), form.getEmailOrCode(), form.getPassword(), form.getPhone(),
                    form.getDateOfBirth(), form.getRoleIds(), form.getAcademicYear(), form.getMajor(), form.getClassName());
            redirectAttributes.addFlashAttribute("successMessage", "Lecturer account updated successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    private void populateDirectory(Model model, UserManagementService.UserDirectoryPage directory, String search) {
        List<UserSummary> users = directory.getContent();
        users.forEach(user -> model.addAttribute("editForm" + user.getId(), toForm(service.getUser(user.getId()))));
        model.addAttribute("pageTitle", "Manage lecturers");
        model.addAttribute("userDirectoryTitle", "Lecturer accounts");
        model.addAttribute("userDirectoryDescription", "View and update lecturer accounts.");
        model.addAttribute("users", users);
        model.addAttribute("activeUserCount", directory.getActiveCount());
        model.addAttribute("lockedUserCount", directory.getLockedCount());
        model.addAttribute("directoryPage", directory);
        model.addAttribute("directorySearch", search == null ? "" : search);
        model.addAttribute("roles", service.listAssignableRoles());
        model.addAttribute("createRoles", service.listAccountCreationRoles());
        model.addAttribute("createStudentAccount", false);
        model.addAttribute("createAccountType", "LECTURER");
        model.addAttribute("statusBasePath", "/admin/lecturers");
        model.addAttribute("editBasePath", "/admin/lecturers");
        model.addAttribute("deleteBasePath", "/admin/lecturers");
    }

    private static LecturerForm toForm(UserEditorData user) {
        LecturerForm form = new LecturerForm();
        form.setLoginIdentifier(user.getLoginIdentifier());
        form.setFullName(user.getFullName());
        form.setEmailOrCode(user.getEmailOrCode());
        form.setPhone(user.getPhone());
        form.setDateOfBirth(user.getDateOfBirth());
        form.setRoleIds(user.getRoleIds());
        return form;
    }
}
