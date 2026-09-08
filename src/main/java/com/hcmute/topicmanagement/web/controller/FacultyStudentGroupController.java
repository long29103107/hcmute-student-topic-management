package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hcmute.topicmanagement.service.StudentGroupService;

@Controller
@RequestMapping("/faculty/groups")
public class FacultyStudentGroupController {

    private final StudentGroupService studentGroupService;

    public FacultyStudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_READ')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "group") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        StudentGroupService.GroupDirectoryPage groupPage = studentGroupService.listFacultyPage(
                authentication.getName(), search, page, size, sort, direction);
        model.addAttribute("pageTitle", "Student groups");
        model.addAttribute("groupPage", groupPage);
        model.addAttribute("groups", groupPage.getGroups());
        model.addAttribute("groupSearch", groupPage.getSearch());
        model.addAttribute("groupSort", groupPage.getSort());
        model.addAttribute("groupDirection", groupPage.getDirection());
        model.addAttribute("groupScope", groupPage.getScopeLabel());
        return "faculty/groups";
    }
}
