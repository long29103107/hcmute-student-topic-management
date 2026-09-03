package com.hcmute.topicmanagement.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_boards")
public class ReviewBoardEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private TopicRegistrationEntity topicRegistration;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewBoardStatus status = ReviewBoardStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "board")
    private Set<ReviewBoardMemberEntity> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "board")
    private Set<EvaluationEntity> evaluations = new LinkedHashSet<>();

    protected ReviewBoardEntity() {
    }

    public ReviewBoardEntity(TopicRegistrationEntity topicRegistration, UserEntity createdBy) {
        this.topicRegistration = topicRegistration;
        this.createdBy = createdBy;
    }

    public TopicRegistrationEntity getTopicRegistration() {
        return topicRegistration;
    }

    public void setTopicRegistration(TopicRegistrationEntity topicRegistration) {
        this.topicRegistration = topicRegistration;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public ReviewBoardStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewBoardStatus status) {
        this.status = status;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public Set<ReviewBoardMemberEntity> getMembers() {
        return members;
    }

    public Set<EvaluationEntity> getEvaluations() {
        return evaluations;
    }
}
