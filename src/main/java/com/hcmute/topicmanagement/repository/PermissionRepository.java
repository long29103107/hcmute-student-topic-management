package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.PermissionEntity;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findByActiveTrueOrderByPermissionGroupAscNameAsc();

    List<PermissionEntity> findByPermissionGroupAndActiveTrueOrderByNameAsc(String permissionGroup);
}
