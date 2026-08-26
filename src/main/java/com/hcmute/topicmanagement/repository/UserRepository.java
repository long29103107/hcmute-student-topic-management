package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLoginIdentifier(String loginIdentifier);

    boolean existsByLoginIdentifier(String loginIdentifier);

    @Query("select distinct u from UserEntity u "
            + "join u.userRoles ur join ur.role r "
            + "where r.code = :roleCode and r.active = true "
            + "and ur.active = true and u.active = true")
    List<UserEntity> findByRoleCodeAndActiveTrue(@Param("roleCode") String roleCode);
}
