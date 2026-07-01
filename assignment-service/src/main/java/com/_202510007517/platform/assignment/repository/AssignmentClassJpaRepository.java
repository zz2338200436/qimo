package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentClassJpaRepository extends JpaRepository<AssignmentClassEntity, AssignmentClassId> {

    List<AssignmentClassEntity> findByClassIdIn(List<Long> classIds);

    void deleteByAssignmentId(Long assignmentId);
}
