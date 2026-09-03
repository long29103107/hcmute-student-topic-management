package com.hcmute.topicmanagement.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.hcmute.topicmanagement.model.enums.TopicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "topics")
public class TopicEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriodEntity registrationPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_by", nullable = false)
    private UserEntity proposedBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TopicStatus status = TopicStatus.DRAFT;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "topic_supervisors",
            joinColumns = @JoinColumn(name = "topic_id"),
            inverseJoinColumns = @JoinColumn(name = "lecturer_id"))
    private Set<UserEntity> supervisors = new LinkedHashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<TopicRegistrationEntity> registrations = new LinkedHashSet<>();

    protected TopicEntity() {
    }

    public TopicEntity(RegistrationPeriodEntity registrationPeriod, DepartmentEntity department,
                       UserEntity proposedBy, String title, String description) {
        this.registrationPeriod = registrationPeriod;
        this.department = department;
        this.proposedBy = proposedBy;
        this.title = title;
        this.description = description;
    }

    public RegistrationPeriodEntity getRegistrationPeriod() {
        return registrationPeriod;
    }

    public void setRegistrationPeriod(RegistrationPeriodEntity registrationPeriod) {
        this.registrationPeriod = registrationPeriod;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public UserEntity getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(UserEntity proposedBy) {
        this.proposedBy = proposedBy;
    }

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

    public TopicStatus getStatus() {
        return status;
    }

    public void setStatus(TopicStatus status) {
        this.status = status;
    }

    public Set<UserEntity> getSupervisors() {
        return supervisors;
    }

    public Set<TopicRegistrationEntity> getRegistrations() {
        return registrations;
    }
}
