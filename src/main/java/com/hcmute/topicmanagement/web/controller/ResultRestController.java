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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.ResultPublicationService;

@RestController
public class ResultRestController {

    private final ResultPublicationService resultPublicationService;

    public ResultRestController(ResultPublicationService resultPublicationService) {
        this.resultPublicationService = resultPublicationService;
    }

    @GetMapping("/api/faculty/results")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public ResultPublicationService.PublicationPage publicationQueue(Authentication authentication) {
        return resultPublicationService.listForPublication(authentication.getName());
    }

    @PostMapping("/api/faculty/results/{id}/publish")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public ResultPublicationService.ResultSummary publish(
            Authentication authentication, @PathVariable Long id) {
        return resultPublicationService.publish(authentication.getName(), id);
    }

    @GetMapping("/api/student/results")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('RESULT_VIEW')")
    public java.util.List<ResultPublicationService.StudentResultSummary> studentResults(
            Authentication authentication) {
        return resultPublicationService.listForStudent(authentication.getName());
    }

    @ExceptionHandler(ResultPublicationService.ResultPublicationValidationException.class)
    public ResponseEntity<ApiError> handleValidation(
            ResultPublicationService.ResultPublicationValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "RESULT_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(ResultPublicationService.ResultPublicationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResultPublicationService.ResultPublicationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "RESULT_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(ResultPublicationService.ResultPublicationAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            ResultPublicationService.ResultPublicationAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "RESULT_FORBIDDEN", exception.getMessage()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
        public ApiError(String code, String message) {
            this(code, message, Map.of());
        }
    }
}
