package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;

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

    @Query("select t.id from TopicEntity t "
            + "join t.registrationPeriod period "
            + "join t.department department "
            + "join t.proposedBy proposer "
            + "where (:scopeDepartmentId is null or department.id = :scopeDepartmentId) "
            + "and (:departmentId is null or department.id = :departmentId) "
            + "and (:periodId is null or period.id = :periodId) "
            + "and (:status is null or t.status = :status) "
            + "and (:search = '' or lower(t.title) like concat('%', :search, '%') "
            + "or lower(department.code) like concat('%', :search, '%') "
            + "or lower(department.name) like concat('%', :search, '%') "
            + "or lower(period.name) like concat('%', :search, '%') "
            + "or lower(str(period.type)) like concat('%', :search, '%') "
            + "or lower(str(t.status)) like concat('%', :search, '%') "
            + "or lower(proposer.fullName) like concat('%', :search, '%') "
            + "or exists (select 1 from t.supervisors supervisor where "
            + "lower(supervisor.fullName) like concat('%', :search, '%') "
            + "or lower(supervisor.emailOrCode) like concat('%', :search, '%')))")
    List<Long> findSupervisorManagementIds(
            @Param("scopeDepartmentId") Long scopeDepartmentId,
            @Param("departmentId") Long departmentId,
            @Param("periodId") Long periodId,
            @Param("status") TopicStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("select t.id from TopicEntity t "
            + "join t.registrationPeriod period "
            + "join t.department department "
            + "join t.proposedBy proposer "
            + "left join t.supervisors sortSupervisor "
            + "where (:scopeDepartmentId is null or department.id = :scopeDepartmentId) "
            + "and (:departmentId is null or department.id = :departmentId) "
            + "and (:periodId is null or period.id = :periodId) "
            + "and (:status is null or t.status = :status) "
            + "and (:search = '' or lower(t.title) like concat('%', :search, '%') "
            + "or lower(department.code) like concat('%', :search, '%') "
            + "or lower(department.name) like concat('%', :search, '%') "
            + "or lower(period.name) like concat('%', :search, '%') "
            + "or lower(str(period.type)) like concat('%', :search, '%') "
            + "or lower(str(t.status)) like concat('%', :search, '%') "
            + "or lower(proposer.fullName) like concat('%', :search, '%') "
            + "or exists (select 1 from t.supervisors supervisor where "
            + "lower(supervisor.fullName) like concat('%', :search, '%') "
            + "or lower(supervisor.emailOrCode) like concat('%', :search, '%'))) "
            + "group by t.id "
            + "order by case when :ascending = true then min(sortSupervisor.fullName) else null end asc, "
            + "case when :ascending = false then min(sortSupervisor.fullName) else null end desc, "
            + "case when :ascending = true then t.id else null end asc, "
            + "case when :ascending = false then t.id else null end desc")
    List<Long> findSupervisorManagementIdsSortedBySupervisor(
            @Param("scopeDepartmentId") Long scopeDepartmentId,
            @Param("departmentId") Long departmentId,
            @Param("periodId") Long periodId,
            @Param("status") TopicStatus status,
            @Param("search") String search,
            @Param("ascending") boolean ascending,
            Pageable pageable);

    @Query("select count(t.id) from TopicEntity t "
            + "join t.registrationPeriod period "
            + "join t.department department "
            + "join t.proposedBy proposer "
            + "where (:scopeDepartmentId is null or department.id = :scopeDepartmentId) "
            + "and (:departmentId is null or department.id = :departmentId) "
            + "and (:periodId is null or period.id = :periodId) "
            + "and (:status is null or t.status = :status) "
            + "and (:search = '' or lower(t.title) like concat('%', :search, '%') "
            + "or lower(department.code) like concat('%', :search, '%') "
            + "or lower(department.name) like concat('%', :search, '%') "
            + "or lower(period.name) like concat('%', :search, '%') "
            + "or lower(str(period.type)) like concat('%', :search, '%') "
            + "or lower(str(t.status)) like concat('%', :search, '%') "
            + "or lower(proposer.fullName) like concat('%', :search, '%') "
            + "or exists (select 1 from t.supervisors supervisor where "
            + "lower(supervisor.fullName) like concat('%', :search, '%') "
            + "or lower(supervisor.emailOrCode) like concat('%', :search, '%')))")
    long countSupervisorManagementTopics(
            @Param("scopeDepartmentId") Long scopeDepartmentId,
            @Param("departmentId") Long departmentId,
            @Param("periodId") Long periodId,
            @Param("status") TopicStatus status,
            @Param("search") String search);

    @Query("select distinct t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "left join fetch t.supervisors "
            + "where t.id in :topicIds")
    List<TopicEntity> findAllByIdForSupervisorManagement(@Param("topicIds") List<Long> topicIds);

    @Query("select distinct department from TopicEntity t join t.department department "
            + "where (:scopeDepartmentId is null or department.id = :scopeDepartmentId) "
            + "order by lower(department.name), department.id")
    List<com.hcmute.topicmanagement.model.DepartmentEntity> findSupervisorManagementDepartments(
            @Param("scopeDepartmentId") Long scopeDepartmentId);

    @Query("select distinct period from TopicEntity t join t.registrationPeriod period join t.department department "
            + "where (:scopeDepartmentId is null or department.id = :scopeDepartmentId) "
            + "order by lower(period.name), period.id")
    List<com.hcmute.topicmanagement.model.RegistrationPeriodEntity> findSupervisorManagementPeriods(
            @Param("scopeDepartmentId") Long scopeDepartmentId);

    @Query("select distinct t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "left join fetch t.supervisors "
            + "where t.id = :topicId")
    Optional<TopicEntity> findByIdForSupervisorManagement(@Param("topicId") Long topicId);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.status = :status "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findByStatusForReview(@Param("status") TopicStatus status);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.department.id = :departmentId and t.status = :status "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findByDepartmentIdAndStatusForReview(
            @Param("departmentId") Long departmentId, @Param("status") TopicStatus status);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.id = :topicId")
    Optional<TopicEntity> findByIdForReview(@Param("topicId") Long topicId);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.id = :topicId")
    Optional<TopicEntity> findByIdForRegistration(@Param("topicId") Long topicId);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.status = :status "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findByStatusForPublication(@Param("status") TopicStatus status);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.department.id = :departmentId and t.status = :status "
            + "order by t.updatedAt desc, t.id desc")
    List<TopicEntity> findByDepartmentIdAndStatusForPublication(
            @Param("departmentId") Long departmentId, @Param("status") TopicStatus status);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod p "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.status = :topicStatus "
            + "and p.status = :periodStatus "
            + "and :now between p.studentRegistrationStart and p.studentRegistrationEnd "
            + "order by lower(t.title), t.id")
    List<TopicEntity> findPublishedForStudent(
            @Param("topicStatus") TopicStatus topicStatus,
            @Param("periodStatus") RegistrationPeriodStatus periodStatus,
            @Param("now") LocalDateTime now);

    @Query("select t from TopicEntity t "
            + "join fetch t.registrationPeriod p "
            + "join fetch t.department "
            + "join fetch t.proposedBy "
            + "where t.status = :topicStatus "
            + "and t.department.id = :departmentId "
            + "and p.status = :periodStatus "
            + "and :now between p.studentRegistrationStart and p.studentRegistrationEnd "
            + "order by lower(t.title), t.id")
    List<TopicEntity> findPublishedForStudentByDepartment(
            @Param("topicStatus") TopicStatus topicStatus,
            @Param("periodStatus") RegistrationPeriodStatus periodStatus,
            @Param("departmentId") Long departmentId,
            @Param("now") LocalDateTime now);

    @Query("select count(supervisor) from TopicEntity topic join topic.supervisors supervisor")
    long countSupervisorAssignments();

    boolean existsByDepartment_Id(Long departmentId);
}
