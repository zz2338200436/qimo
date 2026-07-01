package com._202510007517.platform.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExamJpaRepository extends JpaRepository<ExamEntity, Long> {

    List<ExamEntity> findByTeacherIdOrderByStartTimeDescIdDesc(Long teacherId);

    List<ExamEntity> findByIdIn(Collection<Long> ids);
}
