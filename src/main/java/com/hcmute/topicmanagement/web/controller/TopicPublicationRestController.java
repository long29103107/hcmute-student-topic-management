package com.hcmute.topicmanagement.web.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.topicmanagement.service.TopicPublicationService;
import com.hcmute.topicmanagement.service.TopicPublicationService.PublishedTopicSummary;
import com.hcmute.topicmanagement.service.TopicPublicationService.TopicPublicationSummary;

@RestController
@RequestMapping("/api")
public class TopicPublicationRestController {

    private final TopicPublicationService topicPublicationService;

    public TopicPublicationRestController(TopicPublicationService topicPublicationService) {
        this.topicPublicationService = topicPublicationService;
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('TOPIC_VIEW')")
    public List<PublishedTopicSummary> list(
            Authentication authentication,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long periodId) {
        return topicPublicationService.listPublished(
                authentication.getName(), search, departmentId, periodId, LocalDateTime.now());
    }

    @PostMapping({"/faculty/topics/{id}/publish", "/topics/{id}/publish"})
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY_HEAD') and hasAuthority('TOPIC_REVIEW')")
    public TopicPublicationSummary publish(Authentication authentication, @PathVariable Long id) {
        return topicPublicationService.publish(authentication.getName(), id);
    }

    @ExceptionHandler(TopicPublicationService.TopicPublicationValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(
            TopicPublicationService.TopicPublicationValidationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "TOPIC_PUBLICATION_INVALID", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicPublicationService.TopicPublicationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TopicPublicationService.TopicPublicationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "TOPIC_PUBLICATION_NOT_FOUND", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TopicPublicationService.TopicPublicationAccessException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            TopicPublicationService.TopicPublicationAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "TOPIC_PUBLICATION_FORBIDDEN", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {
    }
}
