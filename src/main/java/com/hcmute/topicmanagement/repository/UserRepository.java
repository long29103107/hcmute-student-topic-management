package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLoginIdentifier(String loginIdentifier);

    @Query("select u from UserEntity u "
            + "where lower(u.emailOrCode) = lower(:email)")
    Optional<UserEntity> findByEmailIgnoreCase(@Param("email") String email);

    @Query("select u from UserEntity u "
            + "left join fetch u.department "
            + "where lower(u.emailOrCode) = lower(:email)")
    Optional<UserEntity> findByEmailIgnoreCaseWithDepartment(@Param("email") String email);

    boolean existsByLoginIdentifierIgnoreCase(String loginIdentifier);

    @Query("select count(u) > 0 from UserEntity u "
            + "where lower(u.emailOrCode) = lower(:emailOrCode)")
    boolean existsByEmailOrCodeIgnoreCase(@Param("emailOrCode") String emailOrCode);

    @Query("select count(u) > 0 from UserEntity u "
            + "where lower(u.emailOrCode) = lower(:emailOrCode) and u.id <> :id")
    boolean existsByEmailOrCodeIgnoreCaseAndIdNot(
            @Param("emailOrCode") String emailOrCode, @Param("id") Long id);

    boolean existsByDepartment_Id(Long departmentId);

    @Query("select count(u) from UserEntity u "
            + "join u.userRoles userRole "
            + "where lower(u.department.code) = lower(:departmentCode) "
            + "and userRole.role.code = :roleCode "
            + "and u.active = true and userRole.active = true")
    long countActiveByDepartmentCodeAndRoleCode(
            @Param("departmentCode") String departmentCode,
            @Param("roleCode") String roleCode);

    @Query("select distinct u from UserEntity u "
            + "left join fetch u.userRoles ur "
            + "left join fetch ur.role "
            + "left join fetch u.department "
            + "order by lower(u.fullName), lower(u.loginIdentifier)")
    List<UserEntity> findAllWithRolesOrderByFullNameAsc();

    @Query("select distinct u from UserEntity u "
            + "left join fetch u.userRoles ur "
            + "left join fetch ur.role "
            + "left join fetch u.department "
            + "where u.id = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);

    @Query("select distinct u from UserEntity u "
            + "left join fetch u.userRoles ur "
            + "left join fetch ur.role "
            + "where u.department.id = :departmentId and u.active = true "
            + "order by lower(u.fullName), lower(u.loginIdentifier)")
    List<UserEntity> findActiveByDepartmentIdWithRolesOrderByFullNameAsc(@Param("departmentId") Long departmentId);

    @Query("select u from UserEntity u "
            + "where lower(u.loginIdentifier) = lower(:identifier) "
            + "or lower(u.emailOrCode) = lower(:identifier)")
    Optional<UserEntity> findByLoginIdentifierOrEmailOrCodeIgnoreCase(
            @Param("identifier") String identifier);

    boolean existsByLoginIdentifier(String loginIdentifier);

    @Query("select distinct u from UserEntity u "
            + "join u.userRoles ur join ur.role r "
            + "where r.code = :roleCode and r.active = true "
            + "and ur.active = true and u.active = true")
    List<UserEntity> findByRoleCodeAndActiveTrue(@Param("roleCode") String roleCode);
}
