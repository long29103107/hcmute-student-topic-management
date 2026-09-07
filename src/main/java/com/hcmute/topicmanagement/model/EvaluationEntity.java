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
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluations")
public class EvaluationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TopicRegistrationEntity topicRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private ReviewBoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_member_id")
    private ReviewBoardMemberEntity boardMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private UserEntity lecturer;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

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

    public ReviewBoardEntity getBoard() {
        return board;
    }

    public void setBoard(ReviewBoardEntity board) {
        this.board = board;
    }

    public ReviewBoardMemberEntity getBoardMember() {
        return boardMember;
    }

    public void setBoardMember(ReviewBoardMemberEntity boardMember) {
        this.boardMember = boardMember;
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

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
