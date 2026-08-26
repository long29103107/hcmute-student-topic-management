package com.hcmute.topicmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluations")
public class EvaluationEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private TopicRegistrationEntity topicRegistration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private UserEntity lecturer;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected EvaluationEntity() {
    }

    public EvaluationEntity(TopicRegistrationEntity topicRegistration, UserEntity lecturer) {
        this.topicRegistration = topicRegistration;
        this.lecturer = lecturer;
    }

    public TopicRegistrationEntity getTopicRegistration() {
        return topicRegistration;
    }

    public void setTopicRegistration(TopicRegistrationEntity topicRegistration) {
        this.topicRegistration = topicRegistration;
    }

    public UserEntity getLecturer() {
        return lecturer;
    }

    public void setLecturer(UserEntity lecturer) {
        this.lecturer = lecturer;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
