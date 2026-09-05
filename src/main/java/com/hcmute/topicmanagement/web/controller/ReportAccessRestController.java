package com.hcmute.topicmanagement.web.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.ReportService;
import com.hcmute.topicmanagement.service.ReportStorage;

@RestController
@RequestMapping("/api/reports")
public class ReportAccessRestController {

    private final ReportService reportService;

    public ReportAccessRestController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ReportService.ReportAccessSummary metadata(
            Authentication authentication, @PathVariable Long reportId) {
        return reportService.metadata(authentication.getName(), reportId);
    }

    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<Resource> download(
            Authentication authentication, @PathVariable Long reportId) {
        return toDownloadResponse(reportService.openDownload(authentication.getName(), reportId));
    }

    private static ResponseEntity<Resource> toDownloadResponse(ReportService.ReportDownload report) {
        ReportService.ReportAccessSummary metadata = report.metadata();
        MediaType contentType = mediaType(metadata.contentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(metadata.originalName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(report.contentLength());
        return ResponseEntity.ok().headers(headers)
                .body(new InputStreamResource(report.content()));
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
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
                .body(new ApiError("REPORT_STORAGE_FAILED", "The report could not be opened."));
    }

    public record ApiError(String code, String message) {
    }
}
