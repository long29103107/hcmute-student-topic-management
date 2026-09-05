package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;

public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    List<EvaluationEntity> findByTopicRegistration_IdOrderByCreatedAtAsc(Long registrationId);

    Optional<EvaluationEntity> findFirstByTopicRegistration_IdOrderByCreatedAtAsc(Long registrationId);

    List<EvaluationEntity> findByLecturer_IdOrderByUpdatedAtDesc(Long lecturerId);

    Optional<EvaluationEntity> findByTopicRegistration_IdAndLecturer_Id(Long registrationId, Long lecturerId);

    boolean existsByTopicRegistration_IdAndLecturer_Id(Long registrationId, Long lecturerId);

    List<EvaluationEntity> findByLecturer_IdAndStatus(Long lecturerId, EvaluationStatus status);

    List<EvaluationEntity> findByBoard_IdAndStatus(Long boardId, EvaluationStatus status);
}
