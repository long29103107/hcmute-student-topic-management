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
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.EvaluationScoringService;
import com.hcmute.topicmanagement.web.dto.EvaluationScoreRequest;

@RestController
@RequestMapping({"/api/lecturer/scoring", "/api/faculty/scores"})
public class EvaluationScoringRestController {

    private final EvaluationScoringService evaluationScoringService;

    public EvaluationScoringRestController(EvaluationScoringService evaluationScoringService) {
        this.evaluationScoringService = evaluationScoringService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public EvaluationScoringService.ScoringPage list(Authentication authentication) {
        return evaluationScoringService.listAssigned(authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EVALUATION_SUBMIT')")
    public EvaluationScoringService.EvaluationSummary submit(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EvaluationScoreRequest request) {
        return evaluationScoringService.submitScore(
                authentication.getName(), id, request.score(), request.comment());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "EVALUATION_INVALID", "Evaluation score is invalid.", fieldErrors));
    }

    @ExceptionHandler(EvaluationScoringService.EvaluationScoringValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            EvaluationScoringService.EvaluationScoringValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "EVALUATION_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(EvaluationScoringService.EvaluationScoringNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            EvaluationScoringService.EvaluationScoringNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "EVALUATION_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(EvaluationScoringService.EvaluationScoringAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            EvaluationScoringService.EvaluationScoringAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "EVALUATION_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
