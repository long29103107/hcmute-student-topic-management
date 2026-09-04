package com.hcmute.topicmanagement.web.controller;

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

import com.hcmute.topicmanagement.service.DepartmentService;
import com.hcmute.topicmanagement.service.UserManagementService;
import com.hcmute.topicmanagement.service.UserManagementService.UserEditorData;
import com.hcmute.topicmanagement.service.UserManagementService.UserSummary;
import com.hcmute.topicmanagement.web.form.PasswordForm;
import com.hcmute.topicmanagement.web.form.StudentForm;

@Controller
@RequestMapping("/admin/students")
public class StudentManagementController {

    private final UserManagementService service;
    private final DepartmentService departmentService;

    public StudentManagementController(UserManagementService service, DepartmentService departmentService) {
        this.service = service;
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "account") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String search, Model model) {
        UserManagementService.UserDirectoryPage directory = service.listUsersPage("STUDENT", search, page, size, sort, direction);
        populateDirectory(model, directory, search);
        StudentForm form = new StudentForm();
        model.addAttribute("studentForm", form);
        model.addAttribute("createForm", form);
        model.addAttribute("createFormName", "studentForm");
        model.addAttribute("formAction", "/admin/students");
        model.addAttribute("statusBasePath", "/admin/students");
        model.addAttribute("editBasePath", "/admin/students");
        model.addAttribute("directoryPath", "/admin/students");
        return "admin/students";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String create(@Valid @ModelAttribute("studentForm") StudentForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (StringUtils.hasText(form.getStudentCode())) {
            form.setLoginIdentifier(form.getStudentCode());
        }
        if (bindingResult.hasErrors()) {
            populateDirectory(model, service.listUsersPage("STUDENT", "", 0, 20, "account", "asc"), "");
            model.addAttribute("studentForm", form);
            model.addAttribute("createForm", form);
            model.addAttribute("createFormName", "studentForm");
            model.addAttribute("formAction", "/admin/students");
            model.addAttribute("statusBasePath", "/admin/students");
            model.addAttribute("editBasePath", "/admin/students");
            return "admin/students";
        }
        try {
            service.createStudent(form.getFullName(), form.getStudentCode(), form.getDepartmentId());
            redirectAttributes.addFlashAttribute("successMessage", "Student account created successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public String toggleStatus(@PathVariable Long id, org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        boolean active = service.toggleActive(id, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", active ? "Student account unlocked successfully." : "Student account locked successfully.");
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String delete(@PathVariable Long id, org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            service.deleteUser(id, authentication.getName(), "STUDENT");
            redirectAttributes.addFlashAttribute("successMessage", "Student account deleted successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
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
            return "redirect:/admin/students";
        }
        try {
            service.setPassword(id, form.getPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Student password set successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE') and hasAuthority('USER_ROLE_ASSIGN')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("editForm") StudentForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the student details.");
            return "redirect:/admin/students";
        }
        try {
            form.setRoleIds(service.getUser(id).getRoleIds());
            service.updateUser(id, form.getFullName(), form.getEmailOrCode(), form.getPassword(),
                    form.getRoleIds(), form.getDepartmentId());
            redirectAttributes.addFlashAttribute("successMessage", "Student account updated successfully.");
        } catch (UserManagementService.UserValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/students";
    }

    private void populateDirectory(Model model, UserManagementService.UserDirectoryPage directory, String search) {
        List<UserSummary> users = directory.getContent();
        users.forEach(user -> model.addAttribute("editForm" + user.getId(), toForm(service.getUser(user.getId()))));
        model.addAttribute("pageTitle", "Manage students");
        model.addAttribute("userDirectoryTitle", "Student accounts");
        model.addAttribute("userDirectoryDescription", "Create and manage student accounts.");
        model.addAttribute("users", users);
        model.addAttribute("activeUserCount", directory.getActiveCount());
        model.addAttribute("lockedUserCount", directory.getLockedCount());
        model.addAttribute("directoryPage", directory);
        model.addAttribute("directorySearch", search == null ? "" : search);
        model.addAttribute("roles", service.listAssignableRoles());
        model.addAttribute("createRoles", service.listAccountCreationRoles());
        model.addAttribute("createStudentAccount", true);
        model.addAttribute("createAccountType", "STUDENT");
        model.addAttribute("departments", departmentService.listDepartmentsForAssignment());
        model.addAttribute("statusBasePath", "/admin/students");
        model.addAttribute("editBasePath", "/admin/students");
        model.addAttribute("deleteBasePath", "/admin/students");
    }

    private static StudentForm toForm(UserEditorData user) {
        StudentForm form = new StudentForm();
        form.setLoginIdentifier(user.getLoginIdentifier());
        form.setFullName(user.getFullName());
        form.setEmailOrCode(user.getEmailOrCode());
        form.setRoleIds(user.getRoleIds());
        form.setStudentCode(user.getStudentCode());
        form.setDepartmentId(user.getDepartmentId());
        return form;
    }
}
