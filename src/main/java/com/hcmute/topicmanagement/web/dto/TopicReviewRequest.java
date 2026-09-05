package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicReviewRequest(@NotBlank(message = "Review decision is required.") String decision) {
}
