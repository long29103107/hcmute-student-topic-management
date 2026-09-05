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

import com.hcmute.topicmanagement.service.TopicRegistrationReviewService;
import com.hcmute.topicmanagement.service.TopicRegistrationReviewService.RegistrationReviewPage;
import com.hcmute.topicmanagement.service.TopicRegistrationReviewService.RegistrationReviewSummary;

@Controller
@RequestMapping("/faculty/registrations/review")
public class TopicRegistrationReviewController {

    private final TopicRegistrationReviewService topicRegistrationReviewService;

    public TopicRegistrationReviewController(TopicRegistrationReviewService topicRegistrationReviewService) {
        this.topicRegistrationReviewService = topicRegistrationReviewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        try {
            RegistrationReviewPage registrationPage = topicRegistrationReviewService.listPendingPage(
                    authentication.getName(), search, page, size, sort, direction);
            model.addAttribute("pageTitle", "Registration review");
            model.addAttribute("registrationPage", registrationPage);
            model.addAttribute("registrations", registrationPage.getRegistrations());
            model.addAttribute("registrationSearch", registrationPage.getSearch());
            return "faculty/registration-review";
        } catch (TopicRegistrationReviewService.TopicRegistrationReviewAccessException exception) {
            return "redirect:/forbidden";
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String review(
            Authentication authentication,
            @RequestParam Long registrationId,
            @RequestParam String decision,
            @RequestParam(required = false) String rejectionReason,
            RedirectAttributes redirectAttributes) {
        try {
            RegistrationReviewSummary result = topicRegistrationReviewService.review(
                    authentication.getName(), registrationId, decision, rejectionReason);
            String action = "APPROVED".equals(result.getStatusCode()) ? "approved" : "rejected";
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Topic registration was " + action + ".");
        } catch (TopicRegistrationReviewService.TopicRegistrationReviewAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicRegistrationReviewService.TopicRegistrationReviewNotFoundException
                | TopicRegistrationReviewService.TopicRegistrationReviewValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/registrations/review";
    }
}
