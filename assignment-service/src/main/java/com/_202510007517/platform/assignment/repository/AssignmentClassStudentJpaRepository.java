package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentClassStudentJpaRepository extends JpaRepository<AssignmentClassStudentEntity, Long> {

    List<AssignmentClassStudentEntity> findByStudentIdOrderByClassId(Long studentId);
}
