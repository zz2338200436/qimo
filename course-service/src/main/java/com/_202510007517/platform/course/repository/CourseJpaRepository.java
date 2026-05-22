package com._202510007517.platform.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findByTeacherIdOrderByIdDesc(Long teacherId);

    List<CourseEntity> findByIdIn(Collection<Long> ids);
}
