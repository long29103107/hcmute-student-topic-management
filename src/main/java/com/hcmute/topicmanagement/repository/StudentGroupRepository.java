package com.hcmute.topicmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;

public interface StudentGroupRepository extends JpaRepository<StudentGroupEntity, Long> {

    List<StudentGroupEntity> findByStatusOrderByCreatedAtDesc(GroupStatus status);

    List<StudentGroupEntity> findByRegistrationPeriod_IdAndStatusOrderByCreatedAtDesc(
            Long periodId, GroupStatus status);

    List<StudentGroupEntity> findByLeader_Id(Long leaderId);

    List<StudentGroupEntity> findByMembers_Id(Long studentId);
}
