package com._202510007517.platform.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassStudentJpaRepository extends JpaRepository<ClassStudentEntity, Long> {

    List<ClassStudentEntity> findByClassIdOrderByStudentId(Long classId);

    List<ClassStudentEntity> findByStudentIdOrderByClassId(Long studentId);

    void deleteByStudentId(Long studentId);
}
