package com.hcmute.topicmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

    List<TopicEntity> findByStatusOrderByCreatedAtDesc(TopicStatus status);

    List<TopicEntity> findByRegistrationPeriod_IdAndStatus(Long periodId, TopicStatus status);

    List<TopicEntity> findByDepartment_IdAndStatus(Long departmentId, TopicStatus status);

    List<TopicEntity> findByProposedBy_IdOrderByCreatedAtDesc(Long lecturerId);
}
