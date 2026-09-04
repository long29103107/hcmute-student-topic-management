package com.hcmute.topicmanagement.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_periods")
public class RegistrationPeriodEntity extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PeriodType type;

    @Column(name = "lecturer_registration_start", nullable = false)
    private LocalDateTime lecturerRegistrationStart;

    @Column(name = "lecturer_registration_end", nullable = false)
    private LocalDateTime lecturerRegistrationEnd;

    @Column(name = "student_registration_start", nullable = false)
    private LocalDateTime studentRegistrationStart;

    @Column(name = "student_registration_end", nullable = false)
    private LocalDateTime studentRegistrationEnd;

    @Column(name = "reviewer_score_deadline")
    private LocalDateTime reviewerScoreDeadline;

    @Column(name = "council_report_date")
    private LocalDateTime councilReportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationPeriodStatus status = RegistrationPeriodStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @OneToMany(mappedBy = "registrationPeriod")
    private Set<TopicEntity> topics = new LinkedHashSet<>();

    @OneToMany(mappedBy = "registrationPeriod")
    private Set<TopicRegistrationEntity> topicRegistrations = new LinkedHashSet<>();

    protected RegistrationPeriodEntity() {
    }

    public RegistrationPeriodEntity(String name, PeriodType type,
                              LocalDateTime lecturerRegistrationStart,
                              LocalDateTime lecturerRegistrationEnd,
                              LocalDateTime studentRegistrationStart,
                              LocalDateTime studentRegistrationEnd) {
        this.name = name;
        this.type = type;
        this.lecturerRegistrationStart = lecturerRegistrationStart;
        this.lecturerRegistrationEnd = lecturerRegistrationEnd;
        this.studentRegistrationStart = studentRegistrationStart;
        this.studentRegistrationEnd = studentRegistrationEnd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PeriodType getType() {
        return type;
    }

    public void setType(PeriodType type) {
        this.type = type;
    }

    public LocalDateTime getLecturerRegistrationStart() {
        return lecturerRegistrationStart;
    }

    public void setLecturerRegistrationStart(LocalDateTime value) {
        lecturerRegistrationStart = value;
    }

    public LocalDateTime getLecturerRegistrationEnd() {
        return lecturerRegistrationEnd;
    }

    public void setLecturerRegistrationEnd(LocalDateTime value) {
        lecturerRegistrationEnd = value;
    }

    public LocalDateTime getStudentRegistrationStart() {
        return studentRegistrationStart;
    }

    public void setStudentRegistrationStart(LocalDateTime value) {
        studentRegistrationStart = value;
    }

    public LocalDateTime getStudentRegistrationEnd() {
        return studentRegistrationEnd;
    }

    public void setStudentRegistrationEnd(LocalDateTime value) {
        studentRegistrationEnd = value;
    }

    public LocalDateTime getReviewerScoreDeadline() {
        return reviewerScoreDeadline;
    }

    public void setReviewerScoreDeadline(LocalDateTime value) {
        reviewerScoreDeadline = value;
    }

    public LocalDateTime getCouncilReportDate() {
        return councilReportDate;
    }

    public void setCouncilReportDate(LocalDateTime value) {
        councilReportDate = value;
    }

    public RegistrationPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationPeriodStatus status) {
        this.status = status;
    }

    /**
     * Registration periods move forward through their lifecycle and cannot be
     * reopened or skip a lifecycle state.
     */
    public boolean canTransitionTo(RegistrationPeriodStatus target) {
        if (target == null || status == null || status == target) {
            return target != null;
        }
        return switch (status) {
            case DRAFT -> target == RegistrationPeriodStatus.OPEN;
            case OPEN -> target == RegistrationPeriodStatus.CLOSED;
            case CLOSED -> target == RegistrationPeriodStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public Set<TopicEntity> getTopics() {
        return topics;
    }

    public Set<TopicRegistrationEntity> getTopicRegistrations() {
        return topicRegistrations;
    }
}
