package com.hcmute.topicmanagement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistrationEntity, Long> {

    List<TopicRegistrationEntity> findByStatusOrderBySubmittedAtDesc(TopicRegistrationStatus status);

    List<TopicRegistrationEntity> findByStudentGroup_IdOrderBySubmittedAtDesc(Long groupId);

    List<TopicRegistrationEntity> findByTopic_IdOrderBySubmittedAtDesc(Long topicId);

    boolean existsByStudentGroup_IdAndRegistrationPeriod_IdAndStatusIn(
            Long groupId, Long periodId, Collection<TopicRegistrationStatus> statuses);

    List<TopicRegistrationEntity> findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(
            Long groupId, Long periodId);

    @Query("select distinct r from TopicRegistrationEntity r "
            + "join fetch r.studentGroup g "
            + "join g.members member "
            + "join fetch r.topic "
            + "join fetch r.registrationPeriod "
            + "join fetch r.submittedBy "
            + "where member.id = :studentId "
            + "order by r.submittedAt desc, r.id desc")
    List<TopicRegistrationEntity> findForStudentWithDetails(@Param("studentId") Long studentId);

    @Query("select distinct r from TopicRegistrationEntity r "
            + "join fetch r.studentGroup g "
            + "join fetch g.leader "
            + "join fetch r.topic t "
            + "join fetch t.department "
            + "join fetch r.registrationPeriod "
            + "join fetch r.submittedBy "
            + "where r.status = :status "
            + "order by r.submittedAt asc, r.id asc")
    List<TopicRegistrationEntity> findByStatusForReview(
            @Param("status") TopicRegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TopicRegistrationEntity r "
            + "join fetch r.studentGroup g "
            + "join fetch g.leader "
            + "join fetch r.topic t "
            + "join fetch t.department "
            + "join fetch r.registrationPeriod "
            + "join fetch r.submittedBy "
            + "where r.id = :id")
    Optional<TopicRegistrationEntity> findByIdForReview(@Param("id") Long id);

    /**
     * Read-only relationship contract for report/evaluation consumers. Only an
     * approved registration is eligible for downstream work.
     */
    @Query("select distinct r from TopicRegistrationEntity r "
            + "join fetch r.studentGroup g "
            + "join fetch g.leader "
            + "join fetch r.topic t "
            + "join fetch t.department "
            + "join fetch r.registrationPeriod "
            + "join fetch r.submittedBy "
            + "where r.id = :id "
            + "and r.status = com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus.APPROVED")
    Optional<TopicRegistrationEntity> findApprovedByIdForReadOnly(@Param("id") Long id);
}
