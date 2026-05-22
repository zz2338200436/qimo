package com._202510007517.platform.course.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaTeacherKnowledgePointRepository implements TeacherKnowledgePointRepository {

    private final TeacherKnowledgePointJpaRepository knowledgePointJpaRepository;
    private final CourseJpaRepository courseJpaRepository;

    public JpaTeacherKnowledgePointRepository(
            TeacherKnowledgePointJpaRepository knowledgePointJpaRepository,
            CourseJpaRepository courseJpaRepository) {
        this.knowledgePointJpaRepository = knowledgePointJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findByTeacherId(Long teacherId) {
        Map<Long, CourseEntity> coursesById = loadCoursesByTeacherId(teacherId);
        return knowledgePointJpaRepository.findByCourseIdInOrderByCourseIdAscOrderIndexAscIdAsc(coursesById.keySet())
                .stream()
                .map(entity -> toRow(entity, coursesById.get(entity.getCourseId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findByTeacherIdAndCourseId(Long teacherId, Long courseId) {
        CourseEntity course = findOwnedCourse(teacherId, courseId).orElse(null);
        if (course == null) {
            return List.of();
        }
        return knowledgePointJpaRepository.findByCourseIdOrderByOrderIndexAscIdAsc(courseId).stream()
                .map(entity -> toRow(entity, course))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findByTeacherIdAndKnowledgePointId(Long teacherId, Long knowledgePointId) {
        return knowledgePointJpaRepository.findById(knowledgePointId)
                .flatMap(entity -> findOwnedCourse(teacherId, entity.getCourseId())
                        .map(course -> toRow(entity, course)));
    }

    @Override
    @Transactional
    public Long insert(Map<String, Object> payload) {
        TeacherKnowledgePointEntity entity = new TeacherKnowledgePointEntity();
        apply(entity, payload);
        return knowledgePointJpaRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void update(Map<String, Object> payload) {
        Long id = toLong(payload.get("id"));
        knowledgePointJpaRepository.findById(id).ifPresent(entity -> {
            apply(entity, payload);
            knowledgePointJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void delete(Long knowledgePointId) {
        knowledgePointJpaRepository.deleteById(knowledgePointId);
    }

    private Map<Long, CourseEntity> loadCoursesByTeacherId(Long teacherId) {
        return courseJpaRepository.findByTeacherIdOrderByIdDesc(teacherId).stream()
                .collect(Collectors.toMap(
                        CourseEntity::getId,
                        course -> course,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Optional<CourseEntity> findOwnedCourse(Long teacherId, Long courseId) {
        return courseJpaRepository.findById(courseId)
                .filter(course -> teacherId != null && teacherId.equals(course.getTeacherId()));
    }

    private Map<String, Object> toRow(TeacherKnowledgePointEntity entity, CourseEntity course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("pointName", entity.getPointName());
        row.put("name", entity.getPointName());
        row.put("description", entity.getDescription());
        row.put("difficulty", entity.getDifficulty());
        row.put("orderIndex", entity.getOrderIndex());
        row.put("courseId", entity.getCourseId());
        row.put("courseName", course == null ? null : course.getCourseName());
        return row;
    }

    private void apply(TeacherKnowledgePointEntity entity, Map<String, Object> payload) {
        entity.setPointName(toStringValue(payload.get("pointName")));
        entity.setDescription(toStringValue(payload.get("description")));
        entity.setDifficulty(toStringValue(payload.get("difficulty")));
        entity.setOrderIndex(toInteger(payload.get("orderIndex")));
        entity.setCourseId(toLong(payload.get("courseId")));
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

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
