package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.ResultPublicationService;

@Controller
public class ResultController {

    private final ResultPublicationService resultPublicationService;

    public ResultController(ResultPublicationService resultPublicationService) {
        this.resultPublicationService = resultPublicationService;
    }

    @GetMapping("/faculty/results")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String publicationQueue(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "group") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        ResultPublicationService.PublicationDirectoryPage publicationPage =
                resultPublicationService.listForPublicationPage(
                        authentication.getName(), search, departmentId, status,
                        page, size, sort, direction);
        model.addAttribute("pageTitle", "Result publication");
        model.addAttribute("publicationPage", publicationPage);
        model.addAttribute("results", publicationPage.results());
        model.addAttribute("resultScope", publicationPage.scopeLabel());
        model.addAttribute("resultSearch", publicationPage.search());
        model.addAttribute("departments", publicationPage.departments());
        model.addAttribute("departmentId", publicationPage.departmentId());
        model.addAttribute("status", publicationPage.status());
        model.addAttribute("resultPage", publicationPage);
        return "faculty/results";
    }

    @PostMapping("/faculty/results/{id}/publish")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String publish(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            resultPublicationService.publish(authentication.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Result published successfully.");
        } catch (ResultPublicationService.ResultPublicationAccessException exception) {
            return "redirect:/forbidden";
        } catch (ResultPublicationService.ResultPublicationNotFoundException
                | ResultPublicationService.ResultPublicationValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/results";
    }

    @GetMapping("/student/results")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('RESULT_VIEW')")
    public String studentResults(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        ResultPublicationService.StudentResultPage resultPage = resultPublicationService.listForStudentPage(
                authentication.getName(), search, departmentId, periodId,
                page, size, sort, direction);
        model.addAttribute("pageTitle", "My results");
        model.addAttribute("resultPage", resultPage);
        model.addAttribute("results", resultPage.results());
        return "student/results";
    }
}
