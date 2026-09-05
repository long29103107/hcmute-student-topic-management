package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicRegistrationReviewRequest(
        @NotBlank(message = "Review decision is required.") String decision,
        @Size(max = 500, message = "Rejection reason must be at most 500 characters.") String rejectionReason) {
}
