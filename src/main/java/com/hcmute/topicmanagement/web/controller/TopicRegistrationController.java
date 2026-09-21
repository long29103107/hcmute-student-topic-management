package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.TopicRegistrationService;
import com.hcmute.topicmanagement.service.TopicRegistrationService.TopicRegistrationForm;
import com.hcmute.topicmanagement.service.TopicRegistrationService.TopicRegistrationPage;
import com.hcmute.topicmanagement.service.TopicRegistrationService.TopicRegistrationSummary;

@Controller
@RequestMapping("/student")
public class TopicRegistrationController {

    private final TopicRegistrationService topicRegistrationService;

    public TopicRegistrationController(TopicRegistrationService topicRegistrationService) {
        this.topicRegistrationService = topicRegistrationService;
    }

    @GetMapping("/groups/register-topic")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public String form(
            Authentication authentication,
            @RequestParam Long groupId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            TopicRegistrationForm registrationForm = topicRegistrationService.registrationForm(
                    authentication.getName(), groupId);
            model.addAttribute("pageTitle", "Register topic");
            model.addAttribute("registrationForm", registrationForm);
            return "student/topic-registration";
        } catch (TopicRegistrationService.TopicRegistrationAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicRegistrationService.TopicRegistrationNotFoundException
                | TopicRegistrationService.TopicRegistrationValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/student/registrations";
        }
    }

    @PostMapping("/groups/register-topic")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public String submit(
            Authentication authentication,
            @RequestParam Long groupId,
            @RequestParam(required = false) Long periodId,
            @RequestParam Long topicId,
            RedirectAttributes redirectAttributes) {
        try {
            TopicRegistrationSummary registration = topicRegistrationService.submit(
                    authentication.getName(), groupId, periodId, topicId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Topic registration for " + registration.getTopicTitle() + " was submitted.");
        } catch (TopicRegistrationService.TopicRegistrationAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicRegistrationService.TopicRegistrationNotFoundException
                | TopicRegistrationService.TopicRegistrationValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/registrations";
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        TopicRegistrationPage registrationPage = topicRegistrationService.listForStudentPage(
                authentication.getName(), search, status, page, size, sort, direction);
        model.addAttribute("pageTitle", "My registrations");
        model.addAttribute("registrationPage", registrationPage);
        model.addAttribute("registrations", registrationPage.getRegistrations());
        return "student/registrations";
    }
}
