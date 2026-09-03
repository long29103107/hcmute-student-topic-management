package com.hcmute.topicmanagement.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistrationEntity, Long> {

    List<TopicRegistrationEntity> findByStatusOrderBySubmittedAtDesc(TopicRegistrationStatus status);

    List<TopicRegistrationEntity> findByStudentGroup_IdOrderBySubmittedAtDesc(Long groupId);

    List<TopicRegistrationEntity> findByTopic_IdOrderBySubmittedAtDesc(Long topicId);

    boolean existsByStudentGroup_IdAndRegistrationPeriod_IdAndStatusIn(
            Long groupId, Long periodId, Collection<TopicRegistrationStatus> statuses);

    List<TopicRegistrationEntity> findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(
            Long groupId, Long periodId);
}
