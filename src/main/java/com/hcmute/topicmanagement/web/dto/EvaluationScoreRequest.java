package com.hcmute.topicmanagement.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvaluationScoreRequest(
        @NotNull(message = "Score is required.") BigDecimal score,
        @Size(max = 10_000, message = "Evaluation comment must be at most 10000 characters.") String comment) {
}
