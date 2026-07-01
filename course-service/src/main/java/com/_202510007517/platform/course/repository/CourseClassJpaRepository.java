package com._202510007517.platform.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseClassJpaRepository extends JpaRepository<CourseClassEntity, Long> {

    List<CourseClassEntity> findByClassNameOrderByIdDesc(String className);
}
