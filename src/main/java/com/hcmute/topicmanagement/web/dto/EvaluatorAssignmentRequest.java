package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EvaluatorAssignmentRequest(
        @NotNull(message = "Evaluator id is required.")
        @Positive(message = "Evaluator id is invalid.")
        Long evaluatorId) {
}
