package com.hcmute.topicmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.UserRoleEntity;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findByUser_Id(Long userId);

    List<UserRoleEntity> findByUser_IdAndActiveTrue(Long userId);

    List<UserRoleEntity> findByRole_IdAndActiveTrue(Long roleId);

    boolean existsByUser_IdAndRole_IdAndActiveTrue(Long userId, Long roleId);

    long countByRole_CodeAndActiveTrueAndUser_ActiveTrue(String roleCode);
}
