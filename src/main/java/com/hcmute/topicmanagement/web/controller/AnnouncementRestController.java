package com.hcmute.topicmanagement.web.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.service.AnnouncementService;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementAccessException;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementNotFoundException;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementSummary;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementValidationException;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementRestController {

    private final AnnouncementService announcementService;

    public AnnouncementRestController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AnnouncementSummary> list(Authentication authentication) {
        return announcementService.listPublished(authentication.getName());
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public List<AnnouncementSummary> manage(Authentication authentication) {
        return announcementService.listForManagement(authentication.getName());
    }

    @PostMapping("/manage")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary create(
            Authentication authentication,
            @RequestBody AnnouncementRequest request) {
        return announcementService.create(
                authentication.getName(),
                request.title(),
                request.content(),
                request.scope(),
                request.departmentId());
    }

    @PutMapping("/manage/{id}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request) {
        return announcementService.update(
                id,
                authentication.getName(),
                request.title(),
                request.content(),
                request.scope(),
                request.departmentId());
    }

    @PostMapping("/manage/{id}/publish")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary publish(Authentication authentication, @PathVariable Long id) {
        return announcementService.publish(authentication.getName(), id);
    }

    @PostMapping("/manage/{id}/hide")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary hide(Authentication authentication, @PathVariable Long id) {
        return announcementService.hide(authentication.getName(), id);
    }

    @ExceptionHandler(AnnouncementValidationException.class)
    public ResponseEntity<ApiError> handleValidation(AnnouncementValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "ANNOUNCEMENT_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(AnnouncementNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AnnouncementNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "ANNOUNCEMENT_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(AnnouncementAccessException.class)
    public ResponseEntity<ApiError> handleAccess(AnnouncementAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "ANNOUNCEMENT_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record AnnouncementRequest(
            String title,
            String content,
            AnnouncementScope scope,
            Long departmentId) {
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
