package com.hcmute.topicmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_results")
public class RegistrationResultEntity {

    @Id
    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TopicRegistrationEntity topicRegistration;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "final_comment", columnDefinition = "TEXT")
    private String finalComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationResultStatus status = RegistrationResultStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by")
    private UserEntity finalizedBy;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private UserEntity publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RegistrationResultEntity() {
    }

    public RegistrationResultEntity(TopicRegistrationEntity topicRegistration) {
        this.topicRegistration = topicRegistration;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    /** The shared primary key is the result identifier. */
    public Long getId() {
        return registrationId;
    }

    public TopicRegistrationEntity getTopicRegistration() {
        return topicRegistration;
    }

    public void setTopicRegistration(TopicRegistrationEntity topicRegistration) {
        this.topicRegistration = topicRegistration;
        this.registrationId = topicRegistration == null ? null : topicRegistration.getId();
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public String getFinalComment() {
        return finalComment;
    }

    public void setFinalComment(String finalComment) {
        this.finalComment = finalComment;
    }

    public RegistrationResultStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationResultStatus status) {
        this.status = status;
    }

    public UserEntity getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(UserEntity finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public UserEntity getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UserEntity publishedBy) {
        this.publishedBy = publishedBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
