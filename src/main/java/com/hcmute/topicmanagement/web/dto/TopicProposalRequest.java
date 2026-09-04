package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TopicProposalRequest(
        @NotBlank(message = "Topic title is required.")
        @Size(max = 255, message = "Topic title must be at most 255 characters.")
        String title,

        @NotBlank(message = "Topic description is required.")
        @Size(max = 5000, message = "Topic description must be at most 5000 characters.")
        String description,

        @NotNull(message = "Department is required.")
        Long departmentId,

        @NotNull(message = "Registration period is required.")
        Long periodId) {
}
