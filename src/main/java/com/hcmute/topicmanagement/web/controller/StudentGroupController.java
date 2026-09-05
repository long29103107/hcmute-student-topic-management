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
import com.hcmute.topicmanagement.service.StudentGroupService.StudentGroupSummary;

@Controller
@RequestMapping("/student/groups")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    public StudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "group") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        StudentGroupService.StudentGroupPage groupPage = studentGroupService.listPage(
                authentication.getName(), search, page, size, sort, direction);
        model.addAttribute("pageTitle", "My groups");
        model.addAttribute("groupPage", groupPage);
        model.addAttribute("groups", groupPage.getGroups());
        model.addAttribute("groupSearch", groupPage.getSearch());
        model.addAttribute("groupSort", groupPage.getSort());
        model.addAttribute("groupDirection", groupPage.getDirection());
        model.addAttribute("periods", studentGroupService.listOpenPeriodOptions());
        return "student/groups";
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String create(
            Authentication authentication,
            @RequestParam String name,
            @RequestParam Long periodId,
            RedirectAttributes redirectAttributes) {
        try {
            StudentGroupSummary group = studentGroupService.create(authentication.getName(), name, periodId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Group " + group.getName() + " created successfully.");
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException
                | StudentGroupService.StudentGroupValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/groups";
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String join(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            StudentGroupSummary group = studentGroupService.join(authentication.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "You joined group " + group.getName() + ".");
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException
                | StudentGroupService.StudentGroupValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/groups";
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String joinByGroupId(
            Authentication authentication,
            @RequestParam Long groupId,
            RedirectAttributes redirectAttributes) {
        return join(authentication, groupId, redirectAttributes);
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String leave(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            StudentGroupSummary group = studentGroupService.leave(authentication.getName(), id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "You left group " + group.getName() + ".");
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException
                | StudentGroupService.StudentGroupValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/groups";
    }

    @PostMapping("/{id}/leader")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public String transferLeader(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Long newLeaderId,
            RedirectAttributes redirectAttributes) {
        try {
            StudentGroupSummary group = studentGroupService.transferLeader(
                    authentication.getName(), id, newLeaderId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Leadership of group " + group.getName() + " was transferred.");
        } catch (StudentGroupService.StudentGroupAccessException exception) {
            return "redirect:/forbidden";
        } catch (StudentGroupService.StudentGroupNotFoundException
                | StudentGroupService.StudentGroupValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/groups";
    }
}
