package com._202510007517.platform.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassCourseJpaRepository extends JpaRepository<ClassCourseEntity, Long> {

    List<ClassCourseEntity> findByClassId(Long classId);

    List<ClassCourseEntity> findByCourseId(Long courseId);

    int deleteByClassIdAndCourseId(Long classId, Long courseId);
}
