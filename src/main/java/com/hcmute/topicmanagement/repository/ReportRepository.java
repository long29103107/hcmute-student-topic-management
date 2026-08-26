package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.ReportEntity;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    List<ReportEntity> findByTopicRegistration_IdOrderBySubmittedAtDesc(Long registrationId);

    Optional<ReportEntity> findFirstByTopicRegistration_IdOrderBySubmittedAtDesc(Long registrationId);
}
