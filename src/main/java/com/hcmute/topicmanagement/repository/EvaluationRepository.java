package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;

public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    Optional<EvaluationEntity> findByTopicRegistration_Id(Long registrationId);

    boolean existsByTopicRegistration_Id(Long registrationId);

    List<EvaluationEntity> findByLecturer_IdAndStatus(Long lecturerId, EvaluationStatus status);
}
