package com.hcmute.topicmanagement.web.dto;

import java.util.List;

public record ReviewBoardRequest(
        Long registrationId,
        String scheduledAt,
        String status,
        List<Long> lecturerIds,
        Long chairId,
        Long secretaryId) {
}