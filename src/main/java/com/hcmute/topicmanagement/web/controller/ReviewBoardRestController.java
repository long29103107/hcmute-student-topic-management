package com.hcmute.topicmanagement.web.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.ReviewBoardService;
import com.hcmute.topicmanagement.web.dto.ReviewBoardRequest;

@RestController
@RequestMapping("/api/faculty/boards")
public class ReviewBoardRestController {

    private final ReviewBoardService reviewBoardService;

    public ReviewBoardRestController(ReviewBoardService reviewBoardService) {
        this.reviewBoardService = reviewBoardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public ReviewBoardService.BoardPage list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "scheduled") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        return reviewBoardService.page(
                authentication.getName(),
                page,
                size,
                departmentId,
                status,
                sort,
                direction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public ReviewBoardService.ReviewBoardSummary get(
            Authentication authentication,
            @PathVariable Long id) {

        return reviewBoardService.get(
                authentication.getName(),
                id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public ReviewBoardService.ReviewBoardSummary create(
            Authentication authentication,
            @RequestBody ReviewBoardRequest request) {

        return save(authentication, null, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public ReviewBoardService.ReviewBoardSummary update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ReviewBoardRequest request) {

        return save(authentication, id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public ReviewBoardService.ReviewBoardSummary status(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        return reviewBoardService.changeStatus(
                authentication.getName(),
                id,
                request.get("status"));
    }

    private ReviewBoardService.ReviewBoardSummary save(
            Authentication authentication,
            Long boardId,
            ReviewBoardRequest request) {

        return reviewBoardService.save(
                authentication.getName(),
                boardId,
                request.registrationId(),
                request.scheduledAt(),
                request.status(),
                request.lecturerIds(),
                request.chairId(),
                request.secretaryId());
    }

    @ExceptionHandler(ReviewBoardService.ReviewBoardValidationException.class)
    public ResponseEntity<ApiError> handleValidation(
            ReviewBoardService.ReviewBoardValidationException exception) {

        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "REVIEW_BOARD_INVALID",
                        exception.getMessage(),
                        Map.of()));
    }

    @ExceptionHandler(ReviewBoardService.ReviewBoardNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ReviewBoardService.ReviewBoardNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        "REVIEW_BOARD_NOT_FOUND",
                        exception.getMessage(),
                        Map.of()));
    }

    @ExceptionHandler(ReviewBoardService.ReviewBoardAccessException.class)
    public ResponseEntity<ApiError> handleAccess(
            ReviewBoardService.ReviewBoardAccessException exception) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(
                        "REVIEW_BOARD_FORBIDDEN",
                        exception.getMessage(),
                        Map.of()));
    }

    public record ApiError(
            String code,
            String message,
            Map<String, String> fieldErrors) {
    }
}