package com.hcmute.topicmanagement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.StudentProfileEntity;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {

    Optional<StudentProfileEntity> findByUser_Id(Long userId);

    boolean existsByStudentCodeIgnoreCase(String studentCode);
}
