package com.hcmute.topicmanagement.web.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hcmute.topicmanagement.service.ReportService;
import com.hcmute.topicmanagement.service.ReportStorage;

@RestController
@RequestMapping("/api/student")
public class ReportRestController {

    private final ReportService reportService;

    public ReportRestController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(value = "/groups/{groupId}/registrations/{registrationId}/reports",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REPORT_SUBMIT')")
    public ResponseEntity<ReportService.ReportSummary> upload(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long registrationId,
            @RequestParam("periodId") Long periodId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.upload(
                authentication.getName(), groupId, registrationId, periodId, file));
    }

    @ExceptionHandler(ReportService.ReportValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ReportService.ReportValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError("REPORT_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(ReportService.ReportNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ReportService.ReportNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("REPORT_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(ReportService.ReportAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(ReportService.ReportAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("REPORT_FORBIDDEN", exception.getMessage()));
    }

    @ExceptionHandler(ReportStorage.ReportStorageException.class)
    public ResponseEntity<ApiError> handleStorageFailure(ReportStorage.ReportStorageException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("REPORT_STORAGE_FAILED", "The report could not be stored."));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
        public ApiError(String code, String message) {
            this(code, message, Map.of());
        }
    }
}
