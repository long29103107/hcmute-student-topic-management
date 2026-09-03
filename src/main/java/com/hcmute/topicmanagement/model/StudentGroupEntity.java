package com.hcmute.topicmanagement.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.hcmute.topicmanagement.model.enums.GroupStatus;
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
@Table(name = "student_groups")
public class StudentGroupEntity extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriodEntity registrationPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private UserEntity leader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status = GroupStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<UserEntity> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studentGroup")
    private Set<TopicRegistrationEntity> topicRegistrations = new LinkedHashSet<>();

    protected StudentGroupEntity() {
    }

    public StudentGroupEntity(String name, RegistrationPeriodEntity registrationPeriod,
            UserEntity createdBy, UserEntity leader) {
        this.name = name;
        this.registrationPeriod = registrationPeriod;
        this.createdBy = createdBy;
        this.leader = leader;
        this.members.add(leader);
    }

    public RegistrationPeriodEntity getRegistrationPeriod() {
        return registrationPeriod;
    }

    public void setRegistrationPeriod(RegistrationPeriodEntity registrationPeriod) {
        this.registrationPeriod = registrationPeriod;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserEntity createdBy) {
        this.createdBy = createdBy;
    }

    public UserEntity getLeader() {
        return leader;
    }

    public void setLeader(UserEntity leader) {
        this.leader = leader;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    public Set<UserEntity> getMembers() {
        return members;
    }

    public Set<TopicRegistrationEntity> getTopicRegistrations() {
        return topicRegistrations;
    }
}
