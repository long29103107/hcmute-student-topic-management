package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

    Optional<TopicEntity> findFirstByTitleIgnoreCase(String title);

    List<TopicEntity> findByStatusOrderByCreatedAtDesc(TopicStatus status);

    List<TopicEntity> findByRegistrationPeriod_IdAndStatus(Long periodId, TopicStatus status);

    List<TopicEntity> findByDepartment_IdAndStatus(Long departmentId, TopicStatus status);

    List<TopicEntity> findByProposedBy_IdOrderByCreatedAtDesc(Long lecturerId);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "where t.proposedBy.id = :lecturerId "
            + "order by t.createdAt desc")
    List<TopicEntity> findOwnProposalsWithPeriodAndDepartment(@Param("lecturerId") Long lecturerId);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "where t.id = :topicId and t.proposedBy.id = :lecturerId")
    Optional<TopicEntity> findOwnProposalWithPeriodAndDepartment(
            @Param("topicId") Long topicId, @Param("lecturerId") Long lecturerId);

    @Query("select distinct t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "left join fetch t.supervisors "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findAllForSupervisorManagement();

    @Query("select distinct t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "left join fetch t.supervisors "
            + "where t.department.id = :departmentId "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findForSupervisorManagementByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("select distinct t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "left join fetch t.supervisors "
            + "where t.id = :topicId")
    Optional<TopicEntity> findByIdForSupervisorManagement(@Param("topicId") Long topicId);

    @Query("select count(supervisor) from TopicEntity topic join topic.supervisors supervisor")
    long countSupervisorAssignments();

    boolean existsByDepartment_Id(Long departmentId);
}
