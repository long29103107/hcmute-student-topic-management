package com.hcmute.topicmanagement.web.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.hcmute.topicmanagement.service.TopicProposalService;
import com.hcmute.topicmanagement.service.TopicProposalService.TopicSummary;
import com.hcmute.topicmanagement.web.form.TopicProposalForm;

@Controller
@RequestMapping("/lecturer/topics")
public class TopicProposalController {

    private final TopicProposalService topicProposalService;

    public TopicProposalController(TopicProposalService topicProposalService) {
        this.topicProposalService = topicProposalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public String list(
            Authentication authentication,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        populatePage(authentication.getName(), model, page, size, search, departmentId, status, sort, direction);
        return "lecturer/topics";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("createForm") TopicProposalForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populatePage(authentication.getName(), model, 0, 5, "", null, "", "updated", "desc");
            model.addAttribute("createForm", form);
            return "lecturer/topics";
        }

        try {
            topicProposalService.create(authentication.getName(), form.getTitle(), form.getDescription(),
                    form.getDepartmentId(), form.getPeriodId());
            redirectAttributes.addFlashAttribute("successMessage", "Topic proposal created successfully.");
        } catch (TopicProposalService.TopicProposalValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") TopicProposalForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the topic proposal details.");
            return "redirect:/lecturer/topics";
        }

        try {
            topicProposalService.update(id, authentication.getName(), form.getTitle(), form.getDescription(),
                    form.getDepartmentId(), form.getPeriodId());
            redirectAttributes.addFlashAttribute("successMessage", "Topic proposal updated successfully.");
        } catch (TopicProposalService.TopicProposalNotFoundException
                | TopicProposalService.TopicProposalValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public String submitForReview(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            topicProposalService.submitForReview(id, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Topic proposal submitted for faculty review.");
        } catch (TopicProposalService.TopicProposalNotFoundException
                | TopicProposalService.TopicProposalValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    private void populatePage(
            String lecturerEmail, Model model, int page, int size, String search, Long departmentId, String status,
            String sort, String direction) {
        TopicProposalService.TopicProposalPage topicPage = topicProposalService.listOwnProposalsPage(
                lecturerEmail, page, size, search, departmentId, status, sort, direction);
        List<TopicSummary> topics = topicPage.getTopics();
        TopicProposalService.ProposalFormOptions options = topicProposalService.getProposalFormOptions();
        model.addAttribute("pageTitle", "My topic proposals");
        model.addAttribute("topicPage", topicPage);
        model.addAttribute("topics", topics);
        model.addAttribute("departments", options.getDepartments());
        model.addAttribute("periods", options.getPeriods());
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new TopicProposalForm());
        }
        topics.stream()
                .filter(TopicSummary::isEditable)
                .forEach(topic -> model.addAttribute("editForm" + topic.getId(), toForm(topic)));
    }

    private static TopicProposalForm toForm(TopicSummary topic) {
        TopicProposalForm form = new TopicProposalForm();
        form.setTitle(topic.getTitle());
        form.setDescription(topic.getDescription());
        form.setDepartmentId(topic.getDepartmentId());
        form.setPeriodId(topic.getPeriodId());
        return form;
    }
}
