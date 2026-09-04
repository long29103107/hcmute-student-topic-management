package com.hcmute.topicmanagement.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_departments_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_departments_name", columnNames = "name")
})
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "department")
    private Set<TopicEntity> topics = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<UserEntity> users = new LinkedHashSet<>();

    protected DepartmentEntity() {
    }

    public DepartmentEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<TopicEntity> getTopics() {
        return topics;
    }

    public Set<UserEntity> getUsers() {
        return users;
    }
}
