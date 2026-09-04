package com.hcmute.topicmanagement.web.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.web.form.DepartmentForm;
import com.hcmute.topicmanagement.service.DepartmentService;

@Controller
@RequestMapping("/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DEPARTMENT_MANAGE')")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        populatePage(model, search, page, size, sort, direction);
        return "faculty/departments";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DEPARTMENT_MANAGE')")
    public String create(
            @Valid @ModelAttribute("createForm") DepartmentForm form,
            BindingResult bindingResult,
            Model model,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populatePage(model, "", 0, 10, "name", "asc");
            model.addAttribute("createForm", form);
            return "faculty/departments";
        }

        try {
            departmentService.create(form.getCode(), form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Department created successfully.");
        } catch (DepartmentService.DepartmentValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DEPARTMENT_MANAGE')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") DepartmentForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the department details.");
            return "redirect:/admin/departments";
        }

        try {
            departmentService.update(id, form.getCode(), form.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Department updated successfully.");
        } catch (DepartmentService.DepartmentNotFoundException
                | DepartmentService.DepartmentValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DEPARTMENT_MANAGE')")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean active = departmentService.toggleActive(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", active ? "Department activated successfully." : "Department deactivated successfully.");
        } catch (DepartmentService.DepartmentNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DEPARTMENT_MANAGE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } catch (DepartmentService.DepartmentNotFoundException
                | DepartmentService.DepartmentValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/departments";
    }

    private void populatePage(Model model, String search, int page, int size, String sort, String direction) {
        model.addAttribute("pageTitle", "Department management");
        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        var departmentPage = departmentService.listDepartmentsPage(
                normalizedSearch, page, size, normalizedSort, normalizedDirection);
        var departments = departmentPage.getContent();
        model.addAttribute("departmentPage", departmentPage);
        model.addAttribute("departmentSearch", normalizedSearch);
        model.addAttribute("departmentSort", departmentPage.getSort());
        model.addAttribute("departmentDirection", departmentPage.getDirection());
        model.addAttribute("departments", departments);
        departments.forEach(department -> {
            DepartmentForm editForm = new DepartmentForm();
            editForm.setCode(department.getCode());
            editForm.setName(department.getName());
            model.addAttribute("editForm" + department.getId(), editForm);
        });
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new DepartmentForm());
        }
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "code", "status" -> sort.trim().toLowerCase(java.util.Locale.ROOT);
            default -> "name";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }
}
