package com.hcmute.topicmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.enums.ReviewBoardMemberRole;

public interface ReviewBoardMemberRepository extends JpaRepository<ReviewBoardMemberEntity, Long> {

    List<ReviewBoardMemberEntity> findByBoard_IdOrderByAssignedAtAsc(Long boardId);

    List<ReviewBoardMemberEntity> findByBoard_IdAndActiveTrueOrderByMemberRoleAscAssignedAtAsc(Long boardId);

    Optional<ReviewBoardMemberEntity> findByBoard_IdAndLecturer_Id(Long boardId, Long lecturerId);

    boolean existsByBoard_IdAndLecturer_Id(Long boardId, Long lecturerId);

    boolean existsByBoard_IdAndLecturer_IdAndActiveTrue(Long boardId, Long lecturerId);

    long countByBoard_Id(Long boardId);

    long countByBoard_IdAndMemberRole(Long boardId, ReviewBoardMemberRole memberRole);

    long countByBoard_IdAndMemberRoleAndActiveTrue(Long boardId, ReviewBoardMemberRole memberRole);
}
