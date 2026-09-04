package com.hcmute.topicmanagement.web.form;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Form model for creating and editing registration periods. */
public class RegistrationPeriodForm {

    @NotBlank(message = "Registration period name is required.")
    @Size(max = 200, message = "Registration period name must be at most 200 characters.")
    private String name;

    @NotNull(message = "Select a period type.")
    private PeriodType type = PeriodType.COURSE;

    @NotNull(message = "Lecturer registration start is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime lecturerRegistrationStart;

    @NotNull(message = "Lecturer registration end is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime lecturerRegistrationEnd;

    @NotNull(message = "Student registration start is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studentRegistrationStart;

    @NotNull(message = "Student registration end is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studentRegistrationEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime reviewerScoreDeadline;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime councilReportDate;

    @NotNull(message = "Select a period status.")
    private RegistrationPeriodStatus status = RegistrationPeriodStatus.DRAFT;

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

    public void setLecturerRegistrationStart(LocalDateTime lecturerRegistrationStart) {
        this.lecturerRegistrationStart = lecturerRegistrationStart;
    }

    public LocalDateTime getLecturerRegistrationEnd() {
        return lecturerRegistrationEnd;
    }

    public void setLecturerRegistrationEnd(LocalDateTime lecturerRegistrationEnd) {
        this.lecturerRegistrationEnd = lecturerRegistrationEnd;
    }

    public LocalDateTime getStudentRegistrationStart() {
        return studentRegistrationStart;
    }

    public void setStudentRegistrationStart(LocalDateTime studentRegistrationStart) {
        this.studentRegistrationStart = studentRegistrationStart;
    }

    public LocalDateTime getStudentRegistrationEnd() {
        return studentRegistrationEnd;
    }

    public void setStudentRegistrationEnd(LocalDateTime studentRegistrationEnd) {
        this.studentRegistrationEnd = studentRegistrationEnd;
    }

    public LocalDateTime getReviewerScoreDeadline() {
        return reviewerScoreDeadline;
    }

    public void setReviewerScoreDeadline(LocalDateTime reviewerScoreDeadline) {
        this.reviewerScoreDeadline = reviewerScoreDeadline;
    }

    public LocalDateTime getCouncilReportDate() {
        return councilReportDate;
    }

    public void setCouncilReportDate(LocalDateTime councilReportDate) {
        this.councilReportDate = councilReportDate;
    }

    public RegistrationPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationPeriodStatus status) {
        this.status = status;
    }
}
