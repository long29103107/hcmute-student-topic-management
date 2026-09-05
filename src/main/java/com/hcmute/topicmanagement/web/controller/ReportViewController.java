package com.hcmute.topicmanagement.web.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hcmute.topicmanagement.service.ReportService;
import com.hcmute.topicmanagement.service.ReportStorage;

@Controller
@RequestMapping("/reports")
public class ReportViewController {

    private final ReportService reportService;

    public ReportViewController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/view")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public Object download(
            Authentication authentication, @RequestParam("id") Long reportId) {
        try {
            ReportService.ReportDownload report = reportService.openDownload(authentication.getName(), reportId);
            return toDownloadResponse(report);
        } catch (ReportService.ReportAccessException exception) {
            return "redirect:/forbidden";
        } catch (ReportService.ReportNotFoundException exception) {
            return "redirect:/not-found";
        } catch (ReportStorage.ReportStorageException exception) {
            return "redirect:/service-unavailable";
        }
    }

    private static ResponseEntity<Resource> toDownloadResponse(ReportService.ReportDownload report) {
        ReportService.ReportAccessSummary metadata = report.metadata();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType(metadata.contentType()));
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
}
