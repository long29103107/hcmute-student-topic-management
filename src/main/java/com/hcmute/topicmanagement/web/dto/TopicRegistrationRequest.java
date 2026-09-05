package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotNull;

public record TopicRegistrationRequest(
        @NotNull(message = "Select a published topic.") Long topicId,
        Long periodId) {
}
