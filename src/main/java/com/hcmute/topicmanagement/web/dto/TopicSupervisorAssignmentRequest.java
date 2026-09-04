package com.hcmute.topicmanagement.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TopicSupervisorAssignmentRequest(
        @NotEmpty(message = "Select at least one supervisor.")
        @Size(max = 2, message = "A topic can have at most two supervisors.")
        List<@NotNull(message = "Supervisor id is required.") @Positive(message = "Supervisor id is invalid.") Long> lecturerIds) {
}
