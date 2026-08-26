package com.hcmute.topicmanagement.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reports")
public class ReportEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TopicRegistrationEntity topicRegistration;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private UserEntity uploader;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected ReportEntity() {
    }

    public ReportEntity(TopicRegistrationEntity topicRegistration, String storedName,
                        String originalName, UserEntity uploader) {
        this.topicRegistration = topicRegistration;
        this.storedName = storedName;
        this.originalName = originalName;
        this.uploader = uploader;
        this.submittedAt = LocalDateTime.now();
    }

    public TopicRegistrationEntity getTopicRegistration() {
        return topicRegistration;
    }

    public void setTopicRegistration(TopicRegistrationEntity topicRegistration) {
        this.topicRegistration = topicRegistration;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public UserEntity getUploader() {
        return uploader;
    }

    public void setUploader(UserEntity uploader) {
        this.uploader = uploader;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
