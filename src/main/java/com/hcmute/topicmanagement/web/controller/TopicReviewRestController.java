package com.hcmute.topicmanagement.web.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.TopicReviewService;
import com.hcmute.topicmanagement.service.TopicReviewService.TopicReviewSummary;
import com.hcmute.topicmanagement.web.dto.TopicReviewRequest;

@RestController
@RequestMapping("/api")
public class TopicReviewRestController {

    private final TopicReviewService topicReviewService;

    public TopicReviewRestController(TopicReviewService topicReviewService) {
        this.topicReviewService = topicReviewService;
    }

    @GetMapping({"/faculty/topics/review", "/topics/review"})
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public List<TopicReviewSummary> list(Authentication authentication) {
        return topicReviewService.listPending(authentication.getName());
    }

    @PostMapping("/topics/{id}/approve")
    @PreAuthorize("hasAuthority('TOPIC_REVIEW')")
    public TopicReviewSummary review(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TopicReviewRequest request) {
        return topicReviewService.review(authentication.getName(), id, request.decision());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_REVIEW_INVALID", "Topic review validation failed.", fieldErrors));
    }

    @ExceptionHandler(TopicReviewService.TopicReviewValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            TopicReviewService.TopicReviewValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_REVIEW_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicReviewService.TopicReviewNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TopicReviewService.TopicReviewNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "TOPIC_REVIEW_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicReviewService.TopicReviewAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            TopicReviewService.TopicReviewAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "TOPIC_REVIEW_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
