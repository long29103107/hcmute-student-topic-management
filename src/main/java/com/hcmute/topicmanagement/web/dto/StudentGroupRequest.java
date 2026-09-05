package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentGroupRequest(
        @NotBlank(message = "Group name is required.")
        @Size(max = 150, message = "Group name must be at most 150 characters.")
        String name,

        @NotNull(message = "Registration period is required.")
        Long periodId) {
}
