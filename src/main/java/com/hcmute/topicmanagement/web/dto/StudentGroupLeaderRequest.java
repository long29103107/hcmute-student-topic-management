package com.hcmute.topicmanagement.web.dto;

import jakarta.validation.constraints.NotNull;

public record StudentGroupLeaderRequest(
        @NotNull(message = "Select a group member as the new leader.") Long newLeaderId) {
}
