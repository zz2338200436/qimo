package com._202510007517.platform.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TeacherKnowledgePointJpaRepository extends JpaRepository<TeacherKnowledgePointEntity, Long> {

    List<TeacherKnowledgePointEntity> findByCourseIdInOrderByCourseIdAscOrderIndexAscIdAsc(Collection<Long> courseIds);

    List<TeacherKnowledgePointEntity> findByCourseIdOrderByOrderIndexAscIdAsc(Long courseId);
}
