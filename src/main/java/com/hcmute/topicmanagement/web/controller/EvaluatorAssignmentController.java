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

import com.hcmute.topicmanagement.service.EvaluatorAssignmentService;

@Controller
@RequestMapping("/faculty/registrations")
public class EvaluatorAssignmentController {

    private final EvaluatorAssignmentService evaluatorAssignmentService;

    public EvaluatorAssignmentController(EvaluatorAssignmentService evaluatorAssignmentService) {
        this.evaluatorAssignmentService = evaluatorAssignmentService;
    }

    @GetMapping("/evaluators")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        EvaluatorAssignmentService.EvaluatorAssignmentPage assignmentPage =
                evaluatorAssignmentService.listManageableRegistrations(
                        authentication.getName(), search, departmentId, page, size, sort, direction);
        model.addAttribute("pageTitle", "Evaluator assignments");
        model.addAttribute("assignmentPage", assignmentPage);
        model.addAttribute("registrations", assignmentPage.registrations());
        model.addAttribute("registrationSearch", assignmentPage.search());
        model.addAttribute("registrationSort", assignmentPage.sort());
        model.addAttribute("registrationDirection", assignmentPage.direction());
        model.addAttribute("registrationDepartmentId", assignmentPage.departmentId());
        model.addAttribute("evaluatorScope", assignmentPage.scopeLabel());
        return "faculty/evaluator-assignments";
    }

    @PostMapping("/{id}/evaluator")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public String assign(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam Long evaluatorId,
            RedirectAttributes redirectAttributes) {
        try {
            evaluatorAssignmentService.assignEvaluator(authentication.getName(), id, evaluatorId);
            redirectAttributes.addFlashAttribute("successMessage", "Evaluator assigned successfully.");
        } catch (EvaluatorAssignmentService.EvaluatorAssignmentAccessException exception) {
            return "redirect:/forbidden";
        } catch (EvaluatorAssignmentService.EvaluatorAssignmentNotFoundException
                | EvaluatorAssignmentService.EvaluatorAssignmentValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/registrations/evaluators";
    }
}
