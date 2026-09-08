package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.StudentGroupService;

@Controller
@RequestMapping("/faculty/groups")
public class FacultyStudentGroupController {

    private final StudentGroupService studentGroupService;

    public FacultyStudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GROUP_READ')")
    public String detail(Authentication authentication, @PathVariable Long id, Model model) {
        try {
            model.addAttribute("pageTitle", "Student group details");
            model.addAttribute("group", studentGroupService.getFacultyDetails(authentication.getName(), id));
            return "faculty/group-detail";
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException exception) {
            return "redirect:/not-found";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('GROUP_UPDATE')")
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam Long leaderId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "group") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String search,
            RedirectAttributes redirectAttributes) {
        try {
            StudentGroupService.StudentGroupSummary group = studentGroupService.updateFacultyGroup(
                    authentication.getName(), id, name, leaderId, status);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Student group " + group.getName() + " updated successfully.");
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException
                | StudentGroupService.StudentGroupValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("direction", direction);
        redirectAttributes.addAttribute("search", search);
        return "redirect:/faculty/groups";
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_READ')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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
