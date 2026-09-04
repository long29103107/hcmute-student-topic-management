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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.service.RegistrationPeriodService;
import com.hcmute.topicmanagement.service.RegistrationPeriodService.PeriodSummary;
import com.hcmute.topicmanagement.web.form.RegistrationPeriodForm;

@Controller
@RequestMapping("/faculty/periods")
public class RegistrationPeriodController {

    private final RegistrationPeriodService registrationPeriodService;

    public RegistrationPeriodController(RegistrationPeriodService registrationPeriodService) {
        this.registrationPeriodService = registrationPeriodService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public String list(Model model) {
        populatePage(model);
        return "faculty/periods";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("createForm") RegistrationPeriodForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populatePage(model);
            model.addAttribute("createForm", form);
            return "faculty/periods";
        }

        try {
            registrationPeriodService.create(
                    authentication.getName(), form.getName(), form.getType(),
                    form.getLecturerRegistrationStart(), form.getLecturerRegistrationEnd(),
                    form.getStudentRegistrationStart(), form.getStudentRegistrationEnd(),
                    form.getReviewerScoreDeadline(), form.getCouncilReportDate(), form.getStatus());
            redirectAttributes.addFlashAttribute("successMessage", "Registration period created successfully.");
        } catch (RegistrationPeriodService.RegistrationPeriodValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/periods";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("editForm") RegistrationPeriodForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the registration period details.");
            return "redirect:/faculty/periods";
        }

        try {
            registrationPeriodService.update(
                    id, form.getName(), form.getType(),
                    form.getLecturerRegistrationStart(), form.getLecturerRegistrationEnd(),
                    form.getStudentRegistrationStart(), form.getStudentRegistrationEnd(),
                    form.getReviewerScoreDeadline(), form.getCouncilReportDate(), form.getStatus());
            redirectAttributes.addFlashAttribute("successMessage", "Registration period updated successfully.");
        } catch (RegistrationPeriodService.RegistrationPeriodNotFoundException
                | RegistrationPeriodService.RegistrationPeriodValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/periods";
    }

    private void populatePage(Model model) {
        List<PeriodSummary> periods = registrationPeriodService.listPeriods();
        model.addAttribute("pageTitle", "Registration period management");
        model.addAttribute("periods", periods);
        model.addAttribute("periodTypes", PeriodType.values());
        model.addAttribute("periodStatuses", RegistrationPeriodStatus.values());
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new RegistrationPeriodForm());
        }
        periods.forEach(period -> model.addAttribute("editForm" + period.getId(), toForm(period)));
    }

    private static RegistrationPeriodForm toForm(PeriodSummary period) {
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setName(period.getName());
        form.setType(period.getType());
        form.setLecturerRegistrationStart(period.getLecturerRegistrationStart());
        form.setLecturerRegistrationEnd(period.getLecturerRegistrationEnd());
        form.setStudentRegistrationStart(period.getStudentRegistrationStart());
        form.setStudentRegistrationEnd(period.getStudentRegistrationEnd());
        form.setReviewerScoreDeadline(period.getReviewerScoreDeadline());
        form.setCouncilReportDate(period.getCouncilReportDate());
        form.setStatus(period.getStatus());
        return form;
    }
}
