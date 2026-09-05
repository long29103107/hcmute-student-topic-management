package com.hcmute.topicmanagement.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriodEntity, Long> {

    List<RegistrationPeriodEntity> findAllByOrderByLecturerRegistrationStartDesc();

    java.util.Optional<RegistrationPeriodEntity> findByNameIgnoreCase(String name);

    List<RegistrationPeriodEntity> findByStatusOrderByStudentRegistrationStartDesc(RegistrationPeriodStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from RegistrationPeriodEntity p where p.id = :id")
    java.util.Optional<RegistrationPeriodEntity> findByIdForGroupMutation(@Param("id") Long id);

    @Query("select p from RegistrationPeriodEntity p "
            + "where p.status = :status "
            + "and :now between p.studentRegistrationStart and p.studentRegistrationEnd")
    List<RegistrationPeriodEntity> findOpenForStudent(@Param("status") RegistrationPeriodStatus status,
                                                      @Param("now") LocalDateTime now);

    @Query("select p from RegistrationPeriodEntity p "
            + "where p.status = :status "
            + "and :now between p.lecturerRegistrationStart and p.lecturerRegistrationEnd")
    List<RegistrationPeriodEntity> findOpenForLecturer(@Param("status") RegistrationPeriodStatus status,
                                                       @Param("now") LocalDateTime now);
}
