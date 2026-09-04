package com.hcmute.topicmanagement.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.TopicSupervisorService;
import com.hcmute.topicmanagement.web.form.TopicSupervisorForm;

@Controller
@RequestMapping("/faculty/topics")
public class TopicSupervisorController {

    private final TopicSupervisorService topicSupervisorService;

    public TopicSupervisorController(TopicSupervisorService topicSupervisorService) {
        this.topicSupervisorService = topicSupervisorService;
    }

    @GetMapping("/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public String list(Authentication authentication, Model model) {
        populatePage(authentication.getName(), model);
        return "faculty/supervisors";
    }

    @PostMapping("/{id}/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public String assign(
            Authentication authentication,
            @PathVariable Long id,
            @ModelAttribute("supervisorForm") TopicSupervisorForm form,
            RedirectAttributes redirectAttributes) {
        try {
            topicSupervisorService.assignSupervisors(authentication.getName(), id, form.getLecturerIds());
            redirectAttributes.addFlashAttribute("successMessage", "Topic supervisors updated successfully.");
        } catch (TopicSupervisorService.TopicSupervisorAccessException exception) {
            return "redirect:/forbidden";
        } catch (TopicSupervisorService.TopicSupervisorNotFoundException
                | TopicSupervisorService.TopicSupervisorValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/topics/supervisors";
    }

    private void populatePage(String actorEmail, Model model) {
        TopicSupervisorService.TopicAssignmentPage page = topicSupervisorService.listManageableTopics(actorEmail);
        model.addAttribute("pageTitle", "Supervisor assignments");
        model.addAttribute("topics", page.getTopics());
        model.addAttribute("supervisorOptions", page.getSupervisorOptions());
        model.addAttribute("supervisorScope", page.getScopeLabel());
        page.getTopics().forEach(topic -> {
            TopicSupervisorForm form = new TopicSupervisorForm();
            form.setLecturerIds(topic.getSupervisorIds());
            model.addAttribute("supervisorForm" + topic.getId(), form);
        });
    }
}
