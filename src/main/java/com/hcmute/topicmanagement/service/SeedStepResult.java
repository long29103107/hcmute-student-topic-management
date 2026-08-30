package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;

public record SeedStepResult(String step, int count, LocalDateTime completedAt) {
}
