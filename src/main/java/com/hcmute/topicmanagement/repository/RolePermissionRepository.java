package com.hcmute.topicmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.RolePermissionEntity;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {

    List<RolePermissionEntity> findByRole_IdAndActiveTrue(Long roleId);

    List<RolePermissionEntity> findByPermission_IdAndActiveTrue(Long permissionId);

    boolean existsByRole_IdAndPermission_IdAndActiveTrue(Long roleId, Long permissionId);
}
