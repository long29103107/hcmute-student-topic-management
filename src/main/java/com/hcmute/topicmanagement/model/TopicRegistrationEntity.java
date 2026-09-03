package com.hcmute.topicmanagement.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
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
@Table(name = "topic_registrations")
public class TopicRegistrationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroupEntity studentGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriodEntity registrationPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private UserEntity submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TopicRegistrationStatus status = TopicRegistrationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "topicRegistration")
    private Set<EvaluationEntity> evaluations = new LinkedHashSet<>();

    @OneToOne(mappedBy = "topicRegistration")
    private ReviewBoardEntity reviewBoard;

    @OneToOne(mappedBy = "topicRegistration")
    private RegistrationResultEntity result;

    protected TopicRegistrationEntity() {
    }

    public TopicRegistrationEntity(StudentGroupEntity studentGroup, TopicEntity topic,
                                   RegistrationPeriodEntity registrationPeriod, UserEntity submittedBy) {
        this.studentGroup = studentGroup;
        this.topic = topic;
        this.registrationPeriod = registrationPeriod;
        this.submittedBy = submittedBy;
        this.submittedAt = LocalDateTime.now();
    }

    public StudentGroupEntity getStudentGroup() {
        return studentGroup;
    }

    public void setStudentGroup(StudentGroupEntity studentGroup) {
        this.studentGroup = studentGroup;
    }

    public TopicEntity getTopic() {
        return topic;
    }

    public void setTopic(TopicEntity topic) {
        this.topic = topic;
    }

    public RegistrationPeriodEntity getRegistrationPeriod() {
        return registrationPeriod;
    }

    public void setRegistrationPeriod(RegistrationPeriodEntity registrationPeriod) {
        this.registrationPeriod = registrationPeriod;
    }

    public UserEntity getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UserEntity submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public TopicRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(TopicRegistrationStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Set<EvaluationEntity> getEvaluations() {
        return evaluations;
    }

    public ReviewBoardEntity getReviewBoard() {
        return reviewBoard;
    }

    public void setReviewBoard(ReviewBoardEntity reviewBoard) {
        this.reviewBoard = reviewBoard;
    }

    public RegistrationResultEntity getResult() {
        return result;
    }

    public void setResult(RegistrationResultEntity result) {
        this.result = result;
    }
}
