package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;

public interface StudentGroupRepository extends JpaRepository<StudentGroupEntity, Long> {

    List<StudentGroupEntity> findByStatusOrderByCreatedAtDesc(GroupStatus status);

    List<StudentGroupEntity> findByRegistrationPeriod_IdAndStatusOrderByCreatedAtDesc(
            Long periodId, GroupStatus status);

    Optional<StudentGroupEntity> findByRegistrationPeriod_IdAndNameIgnoreCase(
            Long periodId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudentGroupEntity g where g.id = :id")
    Optional<StudentGroupEntity> findByIdForMutation(@Param("id") Long id);

    List<StudentGroupEntity> findByLeader_Id(Long leaderId);

    List<StudentGroupEntity> findByMembers_Id(Long studentId);

    @Query("select distinct g from StudentGroupEntity g "
            + "join fetch g.registrationPeriod "
            + "join fetch g.createdBy "
            + "join fetch g.leader "
            + "join g.members relatedMember "
            + "left join fetch g.members "
            + "where relatedMember.id = :studentId")
    List<StudentGroupEntity> findRelatedGroupsWithDetails(@Param("studentId") Long studentId);

    @Query("select distinct g from StudentGroupEntity g "
            + "join fetch g.registrationPeriod "
            + "join fetch g.createdBy "
            + "join fetch g.leader "
            + "left join fetch g.members "
            + "where g.id = :id")
    Optional<StudentGroupEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("select count(g) > 0 from StudentGroupEntity g "
            + "join g.members member "
            + "where g.registrationPeriod.id = :periodId "
            + "and g.status = :status "
            + "and member.id = :studentId")
    boolean existsActiveMembership(
            @Param("periodId") Long periodId,
            @Param("status") GroupStatus status,
            @Param("studentId") Long studentId);
}
