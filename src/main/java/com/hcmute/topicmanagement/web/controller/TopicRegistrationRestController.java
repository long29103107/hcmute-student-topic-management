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

import com.hcmute.topicmanagement.service.TopicRegistrationService;
import com.hcmute.topicmanagement.service.TopicRegistrationService.TopicRegistrationSummary;
import com.hcmute.topicmanagement.web.dto.TopicRegistrationRequest;

@RestController
@RequestMapping("/api/student")
public class TopicRegistrationRestController {

    private final TopicRegistrationService topicRegistrationService;

    public TopicRegistrationRestController(TopicRegistrationService topicRegistrationService) {
        this.topicRegistrationService = topicRegistrationService;
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public List<TopicRegistrationSummary> list(Authentication authentication) {
        return topicRegistrationService.listForStudent(authentication.getName());
    }

    @PostMapping("/groups/{groupId}/registrations")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REGISTRATION_SUBMIT')")
    public ResponseEntity<TopicRegistrationSummary> submit(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody TopicRegistrationRequest request) {
        TopicRegistrationSummary registration = topicRegistrationService.submit(
                authentication.getName(), groupId, request.periodId(), request.topicId());
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_REGISTRATION_INVALID", "Topic registration validation failed.", fieldErrors));
    }

    @ExceptionHandler(TopicRegistrationService.TopicRegistrationValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            TopicRegistrationService.TopicRegistrationValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_REGISTRATION_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicRegistrationService.TopicRegistrationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TopicRegistrationService.TopicRegistrationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "TOPIC_REGISTRATION_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicRegistrationService.TopicRegistrationAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            TopicRegistrationService.TopicRegistrationAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "TOPIC_REGISTRATION_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
