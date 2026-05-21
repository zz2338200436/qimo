package com._202510007517.platform.course.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TeacherKnowledgePointRepository {

    List<Map<String, Object>> findByTeacherId(Long teacherId);

    List<Map<String, Object>> findByTeacherIdAndCourseId(Long teacherId, Long courseId);

    Optional<Map<String, Object>> findByTeacherIdAndKnowledgePointId(Long teacherId, Long knowledgePointId);

    Long insert(Map<String, Object> payload);

    void update(Map<String, Object> payload);

    void delete(Long knowledgePointId);
}
