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

import com.hcmute.topicmanagement.service.TopicReviewService;
import com.hcmute.topicmanagement.service.TopicReviewService.TopicReviewSummary;

@Controller
@RequestMapping("/faculty/topics/review")
public class TopicReviewController {

    private final TopicReviewService topicReviewService;

    public TopicReviewController(TopicReviewService topicReviewService) {
        this.topicReviewService = topicReviewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        model.addAttribute("pageTitle", "Topic review");
        TopicReviewService.TopicReviewPage topicPage = topicReviewService.listPendingPage(
                authentication.getName(), search, departmentId, periodId, page, size, sort, direction);
        model.addAttribute("topicPage", topicPage);
        model.addAttribute("topics", topicPage.getTopics());
        model.addAttribute("topicSearch", topicPage.getSearch());
        model.addAttribute("topicSort", topicPage.getSort());
        model.addAttribute("topicDirection", topicPage.getDirection());
        model.addAttribute("topicDepartmentId", topicPage.getDepartmentId());
        model.addAttribute("topicPeriodId", topicPage.getPeriodId());
        return "faculty/review";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public String review(
            Authentication authentication,
            @RequestParam Long topicId,
            @RequestParam String decision,
            RedirectAttributes redirectAttributes) {
        try {
            TopicReviewSummary result = topicReviewService.review(
                    authentication.getName(), topicId, decision);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Topic proposal " + result.getStatusLabel().toLowerCase() + ".");
        } catch (TopicReviewService.TopicReviewAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicReviewService.TopicReviewNotFoundException
                | TopicReviewService.TopicReviewValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/topics/review";
    }
}
