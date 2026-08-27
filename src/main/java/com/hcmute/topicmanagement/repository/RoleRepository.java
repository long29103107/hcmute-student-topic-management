package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hcmute.topicmanagement.model.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(String code);

    List<RoleEntity> findAllByOrderByNameAsc();

    List<RoleEntity> findByActiveTrueOrderByNameAsc();

    @Query("select distinct r from RoleEntity r "
            + "left join fetch r.rolePermissions rp "
            + "left join fetch rp.permission where r.id = :id")
    Optional<RoleEntity> findByIdWithPermissions(@Param("id") Long id);
}
