package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.AnnouncementEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.model.enums.AnnouncementStatus;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {

    Optional<AnnouncementEntity> findByTitleIgnoreCaseAndScopeAndDepartmentIsNull(
            String title, AnnouncementScope scope);

    Optional<AnnouncementEntity> findByTitleIgnoreCaseAndScopeAndDepartment_Id(
            String title, AnnouncementScope scope, Long departmentId);

    @Query("select a from AnnouncementEntity a "
            + "join fetch a.author left join fetch a.department "
            + "where a.id = :id")
    Optional<AnnouncementEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("select a from AnnouncementEntity a "
            + "join fetch a.author left join fetch a.department "
            + "order by a.updatedAt desc, a.id desc")
    List<AnnouncementEntity> findAllWithDetailsOrderByUpdatedAtDesc();

    @Query("select a from AnnouncementEntity a "
            + "join fetch a.author left join fetch a.department "
            + "where a.status = :status "
            + "and (a.scope = :schoolScope or "
            + "(a.scope = :departmentScope and a.department.id = :departmentId)) "
            + "order by a.publishedAt desc, a.updatedAt desc, a.id desc")
    List<AnnouncementEntity> findPublishedForDepartment(
            @Param("status") AnnouncementStatus status,
            @Param("schoolScope") AnnouncementScope schoolScope,
            @Param("departmentScope") AnnouncementScope departmentScope,
            @Param("departmentId") Long departmentId);

    @Query("select a from AnnouncementEntity a "
            + "join fetch a.author left join fetch a.department "
            + "where a.status = :status "
            + "order by a.publishedAt desc, a.updatedAt desc, a.id desc")
    List<AnnouncementEntity> findPublishedForAdmin(@Param("status") AnnouncementStatus status);
}
