package com.hcmute.topicmanagement.web.controller;

import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.TopicProposalService;
import com.hcmute.topicmanagement.service.TopicProposalService.TopicSummary;
import com.hcmute.topicmanagement.web.dto.TopicProposalRequest;

@RestController
@RequestMapping("/api/lecturer/topics")
public class TopicProposalRestController {

    private final TopicProposalService topicProposalService;

    public TopicProposalRestController(TopicProposalService topicProposalService) {
        this.topicProposalService = topicProposalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public java.util.List<TopicSummary> list(Authentication authentication) {
        return topicProposalService.listOwnProposals(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public ResponseEntity<TopicSummary> create(
            Authentication authentication, @Valid @RequestBody TopicProposalRequest request) {
        TopicSummary topic = topicProposalService.create(authentication.getName(), request.title(),
                request.description(), request.departmentId(), request.periodId());
        return ResponseEntity.status(HttpStatus.CREATED).body(topic);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicSummary update(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody TopicProposalRequest request) {
        return topicProposalService.update(id, authentication.getName(), request.title(), request.description(),
                request.departmentId(), request.periodId());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('TOPIC_PROPOSE')")
    public TopicSummary submitForReview(Authentication authentication, @PathVariable Long id) {
        return topicProposalService.submitForReview(id, authentication.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_PROPOSAL_INVALID", "Topic proposal validation failed.", fieldErrors));
    }

    @ExceptionHandler(TopicProposalService.TopicProposalValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            TopicProposalService.TopicProposalValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_PROPOSAL_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicProposalService.TopicProposalNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TopicProposalService.TopicProposalNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "TOPIC_PROPOSAL_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
