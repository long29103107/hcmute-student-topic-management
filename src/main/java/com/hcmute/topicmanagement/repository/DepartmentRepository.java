package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.DepartmentEntity;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    Optional<DepartmentEntity> findByCode(String code);

    Optional<DepartmentEntity> findByCodeIgnoreCase(String code);

    Optional<DepartmentEntity> findByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<DepartmentEntity> findAllByOrderByNameAsc();

    List<DepartmentEntity> findByActiveTrueOrderByNameAsc();
}
