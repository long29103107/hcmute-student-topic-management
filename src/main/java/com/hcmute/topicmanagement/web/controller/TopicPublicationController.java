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

import com.hcmute.topicmanagement.service.TopicPublicationService;
import com.hcmute.topicmanagement.service.TopicPublicationService.TopicPublicationSummary;

@Controller
@RequestMapping("/faculty/topics/publish")
public class TopicPublicationController {

    private final TopicPublicationService topicPublicationService;

    public TopicPublicationController(TopicPublicationService topicPublicationService) {
        this.topicPublicationService = topicPublicationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_HEAD') and hasAuthority('TOPIC_REVIEW')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        TopicPublicationService.TopicPublicationPage topicPage = topicPublicationService.listApprovedPage(
                authentication.getName(), search, page, size, sort, direction);
        model.addAttribute("pageTitle", "Publish topics");
        model.addAttribute("topicPage", topicPage);
        model.addAttribute("topics", topicPage.getTopics());
        model.addAttribute("topicSearch", topicPage.getSearch());
        model.addAttribute("topicSort", topicPage.getSort());
        model.addAttribute("topicDirection", topicPage.getDirection());
        return "faculty/publish";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_HEAD') and hasAuthority('TOPIC_REVIEW')")
    public String publish(
            Authentication authentication,
            @RequestParam Long topicId,
            RedirectAttributes redirectAttributes) {
        try {
            TopicPublicationSummary result = topicPublicationService.publish(
                    authentication.getName(), topicId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Topic " + result.getTitle() + " published successfully.");
        } catch (TopicPublicationService.TopicPublicationAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicPublicationService.TopicPublicationNotFoundException
                | TopicPublicationService.TopicPublicationValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/topics/publish";
    }
}
