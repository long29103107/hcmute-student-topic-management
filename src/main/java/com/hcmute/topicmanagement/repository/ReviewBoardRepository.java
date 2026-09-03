package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;

public interface ReviewBoardRepository extends JpaRepository<ReviewBoardEntity, Long> {

    Optional<ReviewBoardEntity> findByTopicRegistration_Id(Long registrationId);

    boolean existsByTopicRegistration_Id(Long registrationId);

    List<ReviewBoardEntity> findByStatusOrderByScheduledAtAsc(ReviewBoardStatus status);
}
