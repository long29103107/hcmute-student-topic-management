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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.TopicSupervisorService;
import com.hcmute.topicmanagement.service.TopicSupervisorService.TopicAssignmentPage;
import com.hcmute.topicmanagement.service.TopicSupervisorService.TopicSummary;
import com.hcmute.topicmanagement.web.dto.TopicSupervisorAssignmentRequest;

@RestController
@RequestMapping("/api/faculty/topics")
public class TopicSupervisorRestController {

    private final TopicSupervisorService topicSupervisorService;

    public TopicSupervisorRestController(TopicSupervisorService topicSupervisorService) {
        this.topicSupervisorService = topicSupervisorService;
    }

    @GetMapping("/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicAssignmentPage list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return topicSupervisorService.listManageableTopics(
                authentication.getName(), search, page, size, sort, direction);
    }

    @PutMapping("/{id}/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_MANAGE')")
    public TopicSummary assign(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TopicSupervisorAssignmentRequest request) {
        return topicSupervisorService.assignSupervisors(authentication.getName(), id, request.lecturerIds());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_SUPERVISOR_INVALID", "Topic supervisor assignment is invalid.", fieldErrors));
    }

    @ExceptionHandler(TopicSupervisorService.TopicSupervisorValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            TopicSupervisorService.TopicSupervisorValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_SUPERVISOR_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicSupervisorService.TopicSupervisorNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TopicSupervisorService.TopicSupervisorNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "TOPIC_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicSupervisorService.TopicSupervisorAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            TopicSupervisorService.TopicSupervisorAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "TOPIC_SUPERVISOR_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
