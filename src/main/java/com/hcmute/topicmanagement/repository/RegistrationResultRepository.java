package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;

public interface RegistrationResultRepository extends JpaRepository<RegistrationResultEntity, Long> {

    Optional<RegistrationResultEntity> findByTopicRegistration_Id(Long registrationId);

    List<RegistrationResultEntity> findByStatusOrderByUpdatedAtDesc(RegistrationResultStatus status);
}
