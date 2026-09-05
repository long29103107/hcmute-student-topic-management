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

import com.hcmute.topicmanagement.service.EvaluatorAssignmentService;
import com.hcmute.topicmanagement.web.dto.EvaluatorAssignmentRequest;

@RestController
@RequestMapping("/api/faculty/registrations")
public class EvaluatorAssignmentRestController {

    private final EvaluatorAssignmentService evaluatorAssignmentService;

    public EvaluatorAssignmentRestController(EvaluatorAssignmentService evaluatorAssignmentService) {
        this.evaluatorAssignmentService = evaluatorAssignmentService;
    }

    @GetMapping("/evaluators")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public EvaluatorAssignmentService.EvaluatorAssignmentPage list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "topic") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return evaluatorAssignmentService.listManageableRegistrations(
                authentication.getName(), search, page, size, sort, direction);
    }

    @PutMapping("/{id}/evaluator")
    @PreAuthorize("hasAuthority('REGISTRATION_REVIEW')")
    public EvaluatorAssignmentService.RegistrationSummary assign(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EvaluatorAssignmentRequest request) {
        return evaluatorAssignmentService.assignEvaluator(
                authentication.getName(), id, request.evaluatorId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "EVALUATOR_ASSIGNMENT_INVALID", "Evaluator assignment is invalid.", fieldErrors));
    }

    @ExceptionHandler(EvaluatorAssignmentService.EvaluatorAssignmentValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            EvaluatorAssignmentService.EvaluatorAssignmentValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "EVALUATOR_ASSIGNMENT_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(EvaluatorAssignmentService.EvaluatorAssignmentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            EvaluatorAssignmentService.EvaluatorAssignmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "EVALUATOR_ASSIGNMENT_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(EvaluatorAssignmentService.EvaluatorAssignmentAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            EvaluatorAssignmentService.EvaluatorAssignmentAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "EVALUATOR_ASSIGNMENT_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
