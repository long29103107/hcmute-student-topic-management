package com.hcmute.topicmanagement.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriodEntity, Long> {

    List<RegistrationPeriodEntity> findByStatusOrderByStudentRegistrationStartDesc(RegistrationPeriodStatus status);

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
