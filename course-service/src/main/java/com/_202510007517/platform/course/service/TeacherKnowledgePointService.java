package com._202510007517.platform.course.service;

import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.course.repository.CourseRepository;
import com._202510007517.platform.course.repository.TeacherKnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TeacherKnowledgePointService {

    private final TeacherKnowledgePointRepository teacherKnowledgePointRepository;
    private final CourseRepository courseRepository;

    public TeacherKnowledgePointService(TeacherKnowledgePointRepository teacherKnowledgePointRepository,
                                        CourseRepository courseRepository) {
        this.teacherKnowledgePointRepository = teacherKnowledgePointRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listKnowledgePoints(Long teacherId, Long courseId) {
        return courseId == null
                ? teacherKnowledgePointRepository.findByTeacherId(teacherId)
                : listKnowledgePointsByCourse(teacherId, courseId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listKnowledgePointsByCourse(Long teacherId, Long courseId) {
        ensureTeacherOwnsCourse(teacherId, courseId);
        return teacherKnowledgePointRepository.findByTeacherIdAndCourseId(teacherId, courseId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getKnowledgePoint(Long teacherId, Long knowledgePointId) {
        return teacherKnowledgePointRepository.findByTeacherIdAndKnowledgePointId(teacherId, knowledgePointId)
                .orElseThrow(() -> new ResourceNotFoundException("知识点不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createKnowledgePoint(Long teacherId, Map<String, Object> payload) {
        Long courseId = toLong(payload.get("courseId"));
        ensureTeacherOwnsCourse(teacherId, courseId);
        Map<String, Object> values = normalizePayload(payload);
        Long id = teacherKnowledgePointRepository.insert(values);
        return getKnowledgePoint(teacherId, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateKnowledgePoint(Long teacherId, Long knowledgePointId, Map<String, Object> payload) {
        getKnowledgePoint(teacherId, knowledgePointId);
        Long courseId = toLong(payload.get("courseId"));
        ensureTeacherOwnsCourse(teacherId, courseId);
        Map<String, Object> values = normalizePayload(payload);
        values.put("id", knowledgePointId);
        teacherKnowledgePointRepository.update(values);
        return getKnowledgePoint(teacherId, knowledgePointId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgePoint(Long teacherId, Long knowledgePointId) {
        getKnowledgePoint(teacherId, knowledgePointId);
        teacherKnowledgePointRepository.delete(knowledgePointId);
    }

    private void ensureTeacherOwnsCourse(Long teacherId, Long courseId) {
        if (courseId == null || !courseRepository.teacherOwnsCourse(teacherId, courseId)) {
            throw new IllegalArgumentException("无权访问该课程");
        }
    }

    private static Map<String, Object> normalizePayload(Map<String, Object> payload) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("pointName", firstNonBlank(payload.get("pointName"), payload.get("name")));
        values.put("description", Objects.toString(payload.get("description"), null));
        values.put("difficulty", Objects.toString(payload.get("difficulty"), null));
        values.put("orderIndex", toInteger(payload.get("orderIndex")));
        values.put("courseId", toLong(payload.get("courseId")));
        return values;
    }

    private static String firstNonBlank(Object first, Object second) {
        String firstValue = first != null ? first.toString() : null;
        if (firstValue != null && !firstValue.isBlank()) {
            return firstValue;
        }
        return second != null ? second.toString() : null;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}
