package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

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

    boolean existsByDepartment_Id(Long departmentId);
}
