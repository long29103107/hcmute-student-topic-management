package com.hcmute.topicmanagement.web.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.service.ReportService;
import com.hcmute.topicmanagement.service.ReportStorage;

@Controller
@RequestMapping("/student")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/registrations/{registrationId}/report")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('REPORT_SUBMIT')")
    public String upload(
            Authentication authentication,
            @PathVariable Long registrationId,
            @RequestParam("groupId") Long groupId,
            @RequestParam("periodId") Long periodId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile legacyFile,
            @RequestParam(value = "removeReportIds", required = false) List<Long> removeReportIds,
            RedirectAttributes redirectAttributes) {
        try {
            List<MultipartFile> uploadedFiles = files != null
                    ? files
                    : (legacyFile == null ? List.of() : List.of(legacyFile));
            List<ReportService.ReportSummary> reports = reportService.upload(
                    authentication.getName(), groupId, registrationId, periodId, uploadedFiles,
                    removeReportIds == null ? List.of() : removeReportIds);
            redirectAttributes.addFlashAttribute(
                    "successMessage", reports.size() + " report file(s) uploaded successfully.");
        } catch (ReportService.ReportAccessException exception) {
            return "redirect:/forbidden";
        } catch (ReportService.ReportNotFoundException
                | ReportService.ReportValidationException
                | ReportStorage.ReportStorageException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/registrations";
    }
}
