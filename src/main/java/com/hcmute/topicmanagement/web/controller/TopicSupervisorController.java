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
import org.springframework.web.bind.annotation.RequestParam;
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
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        populatePage(authentication.getName(), model, search, departmentId, periodId, status, page, size, sort, direction);
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

    private void populatePage(
            String actorEmail, Model model, String search, Long departmentId, Long periodId, String status,
            int page, int size, String sort, String direction) {
        TopicSupervisorService.TopicAssignmentPage topicPage =
                topicSupervisorService.listManageableTopics(
                        actorEmail, search, departmentId, periodId, status, page, size, sort, direction);
        model.addAttribute("pageTitle", "Supervisor assignments");
        model.addAttribute("topicPage", topicPage);
        model.addAttribute("topics", topicPage.getTopics());
        model.addAttribute("topicSearch", topicPage.getSearch());
        model.addAttribute("topicDepartmentId", topicPage.getDepartmentId());
        model.addAttribute("topicPeriodId", topicPage.getPeriodId());
        model.addAttribute("topicStatus", topicPage.getStatus());
        model.addAttribute("topicSort", topicPage.getSort());
        model.addAttribute("topicDirection", topicPage.getDirection());
        model.addAttribute("supervisorScope", topicPage.getScopeLabel());
        topicPage.getTopics().forEach(topic -> {
            TopicSupervisorForm form = new TopicSupervisorForm();
            form.setLecturerIds(topic.getSupervisorIds());
            model.addAttribute("supervisorForm" + topic.getId(), form);
        });
    }
}
