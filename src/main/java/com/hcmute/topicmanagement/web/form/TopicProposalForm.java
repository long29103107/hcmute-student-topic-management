package com.hcmute.topicmanagement.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Form model for a lecturer's topic proposal. */
public class TopicProposalForm {

    @NotBlank(message = "Topic title is required.")
    @Size(max = 255, message = "Topic title must be at most 255 characters.")
    private String title;

    @NotBlank(message = "Topic description is required.")
    @Size(max = 5000, message = "Topic description must be at most 5000 characters.")
    private String description;

    @NotNull(message = "Select a department.")
    private Long departmentId;

    @NotNull(message = "Select a registration period.")
    private Long periodId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }
}
