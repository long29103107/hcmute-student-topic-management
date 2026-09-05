package com.hcmute.topicmanagement.web.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.StudentGroupService;
import com.hcmute.topicmanagement.service.StudentGroupService.StudentGroupPage;
import com.hcmute.topicmanagement.service.StudentGroupService.StudentGroupSummary;
import com.hcmute.topicmanagement.web.dto.StudentGroupLeaderRequest;
import com.hcmute.topicmanagement.web.dto.StudentGroupRequest;

@RestController
@RequestMapping("/api/student/groups")
public class StudentGroupRestController {

    private final StudentGroupService studentGroupService;

    public StudentGroupRestController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupPage list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "group") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return studentGroupService.listPage(
                authentication.getName(), search, page, size, sort, direction);
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public ResponseEntity<StudentGroupSummary> create(
            Authentication authentication, @Valid @RequestBody StudentGroupRequest request) {
        StudentGroupSummary group = studentGroupService.create(
                authentication.getName(), request.name(), request.periodId());
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary join(Authentication authentication, @PathVariable Long id) {
        return studentGroupService.join(authentication.getName(), id);
    }

    @DeleteMapping("/{id}/members/me")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary leave(Authentication authentication, @PathVariable Long id) {
        return studentGroupService.leave(authentication.getName(), id);
    }

    @PutMapping("/{id}/leader")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    public StudentGroupSummary transferLeader(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody StudentGroupLeaderRequest request) {
        return studentGroupService.transferLeader(authentication.getName(), id, request.newLeaderId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                "STUDENT_GROUP_INVALID", "Student group validation failed.", fieldErrors));
    }

    @ExceptionHandler(StudentGroupService.StudentGroupValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            StudentGroupService.StudentGroupValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "STUDENT_GROUP_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(StudentGroupService.StudentGroupNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            StudentGroupService.StudentGroupNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "STUDENT_GROUP_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(StudentGroupService.StudentGroupAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            StudentGroupService.StudentGroupAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "STUDENT_GROUP_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
