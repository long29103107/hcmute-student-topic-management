package com.hcmute.topicmanagement.model.enums;

public enum ReviewBoardStatus {
    DRAFT,
    ASSIGNED,
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    PUBLISHED,
    CLOSED;

    public boolean canTransitionTo(ReviewBoardStatus target) {
        if (target == null) {
            return false;
        }
        if (target == this) {
            return true;
        }
        return switch (this) {
            case DRAFT -> target == ASSIGNED || target == SCHEDULED || target == ACTIVE;
            case ASSIGNED -> target == SCHEDULED || target == ACTIVE;
            case SCHEDULED -> target == ACTIVE || target == COMPLETED;
            case ACTIVE -> target == COMPLETED;
            case COMPLETED -> target == PUBLISHED || target == CLOSED;
            case PUBLISHED -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
