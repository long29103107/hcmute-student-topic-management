package com.hcmute.topicmanagement.model;

import java.time.LocalDateTime;

import com.hcmute.topicmanagement.model.enums.ReviewBoardMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "review_board_members", uniqueConstraints = @UniqueConstraint(
        name = "uk_review_board_members_board_lecturer", columnNames = {"board_id", "lecturer_id"}))
public class ReviewBoardMemberEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private ReviewBoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private UserEntity lecturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private ReviewBoardMemberRole memberRole = ReviewBoardMemberRole.MEMBER;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected ReviewBoardMemberEntity() {
    }

    public ReviewBoardMemberEntity(ReviewBoardEntity board, UserEntity lecturer) {
        this.board = board;
        this.lecturer = lecturer;
        this.assignedAt = LocalDateTime.now();
    }

    public ReviewBoardEntity getBoard() {
        return board;
    }

    public void setBoard(ReviewBoardEntity board) {
        this.board = board;
    }

    public UserEntity getLecturer() {
        return lecturer;
    }

    public void setLecturer(UserEntity lecturer) {
        this.lecturer = lecturer;
    }

    public ReviewBoardMemberRole getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(ReviewBoardMemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
