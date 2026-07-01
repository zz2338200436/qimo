package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, Long> {

    List<AssignmentEntity> findByCourseIdOrderByIdDesc(Long courseId);

    List<AssignmentEntity> findByTeacherIdOrderByIdDesc(Long teacherId);

    List<AssignmentEntity> findByIdIn(Collection<Long> ids);
}
