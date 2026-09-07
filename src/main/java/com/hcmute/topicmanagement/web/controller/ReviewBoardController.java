package com.hcmute.topicmanagement.web.controller;

import java.util.List;

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

import com.hcmute.topicmanagement.service.ReviewBoardService;

@Controller
@RequestMapping("/faculty/boards")
public class ReviewBoardController {

    private final ReviewBoardService reviewBoardService;

    public ReviewBoardController(ReviewBoardService reviewBoardService) {
        this.reviewBoardService = reviewBoardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public String list(Authentication authentication, Model model) {
        ReviewBoardService.BoardPage page = reviewBoardService.page(authentication.getName());
        model.addAttribute("pageTitle", "Review boards");
        model.addAttribute("boardPage", page);
        model.addAttribute("boards", page.boards());
        model.addAttribute("boardRegistrations", page.registrations());
        model.addAttribute("boardLecturers", page.lecturerOptions());
        model.addAttribute("boardScope", page.scopeLabel());
        return "faculty/boards";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public String save(
            Authentication authentication,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long registrationId,
            @RequestParam(required = false) String scheduledAt,
            @RequestParam(defaultValue = "DRAFT") String status,
            @RequestParam(required = false) List<Long> lecturerIds,
            @RequestParam(required = false) Long chairId,
            @RequestParam(required = false) Long secretaryId,
            RedirectAttributes redirectAttributes) {
        try {
            reviewBoardService.save(authentication.getName(), boardId, registrationId, scheduledAt, status,
                    lecturerIds, chairId, secretaryId);
            redirectAttributes.addFlashAttribute("successMessage", boardId == null
                    ? "Review board created successfully." : "Review board updated successfully.");
        } catch (ReviewBoardService.ReviewBoardAccessException exception) {
            return "redirect:/forbidden";
        } catch (ReviewBoardService.ReviewBoardNotFoundException
                | ReviewBoardService.ReviewBoardValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/boards";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public String changeStatus(Authentication authentication, @PathVariable Long id,
                               @RequestParam String status, RedirectAttributes redirectAttributes) {
        try {
            reviewBoardService.changeStatus(authentication.getName(), id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Review board status updated.");
        } catch (ReviewBoardService.ReviewBoardAccessException exception) {
            return "redirect:/forbidden";
        } catch (ReviewBoardService.ReviewBoardNotFoundException
                | ReviewBoardService.ReviewBoardValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/faculty/boards";
    }
}