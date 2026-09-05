package com.hcmute.topicmanagement.web.controller;

import java.math.BigDecimal;

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

import com.hcmute.topicmanagement.service.EvaluationScoringService;

@Controller
@RequestMapping("/lecturer")
public class EvaluationScoringController {

    private final EvaluationScoringService evaluationScoringService;

    public EvaluationScoringController(EvaluationScoringService evaluationScoringService) {
        this.evaluationScoringService = evaluationScoringService;
    }

    @GetMapping("/scoring")
    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public String list(Authentication authentication, Model model) {
        EvaluationScoringService.ScoringPage scoringPage =
                evaluationScoringService.listAssigned(authentication.getName());
        model.addAttribute("pageTitle", "My evaluations");
        model.addAttribute("scoringPage", scoringPage);
        model.addAttribute("evaluations", scoringPage.evaluations());
        model.addAttribute("minimumScore", scoringPage.minimumScore());
        model.addAttribute("maximumScore", scoringPage.maximumScore());
        return "lecturer/scoring";
    }

    @PostMapping("/scoring/{id}")
    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public String submit(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal score,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes) {
        try {
            evaluationScoringService.submitScore(authentication.getName(), id, score, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Evaluation saved successfully.");
        } catch (EvaluationScoringService.EvaluationScoringAccessException exception) {
            return "redirect:/forbidden";
        } catch (EvaluationScoringService.EvaluationScoringNotFoundException
                | EvaluationScoringService.EvaluationScoringValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/lecturer/scoring";
    }
}
