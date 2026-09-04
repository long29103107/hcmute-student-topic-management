package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hcmute.topicmanagement.service.DepartmentService;

@Controller
@RequestMapping("/faculty/departments")
public class FacultyDepartmentController {

    private final DepartmentService departmentService;

    public FacultyDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_HEAD')")
    public String viewMyDepartment(Authentication authentication, Model model) {
        model.addAttribute("pageTitle", "My department");
        model.addAttribute("department", departmentService
                .getDepartmentForFacultyHead(authentication.getName()));
        return "faculty/my-department";
    }
}
