package com._202510007517.platform.course.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCourseRepositoryTest {

    @Test
    void findClassesByTeacherIdKeepsSpaceBeforeGroupByWhenGradeFilterPresent() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of()));
        JdbcCourseRepository repository = new JdbcCourseRepository(jdbcTemplate);

        repository.findClassesByTeacherId(7L, "ClassCrudSmoke0516A", "2026", null, 2L, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(MapSqlParameterSource.class));

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("AND cc.class_name LIKE :className");
        assertThat(sql).contains("AND cc.year = :grade");
        assertThat(sql).contains("AND cc.major_id = :majorId");
        assertThat(sql).containsPattern(":grade\\s+AND cc\\.major_id = :majorId\\s+GROUP BY");
    }

    @Test
    void classNameExistsExcludesCurrentClassId() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);
        JdbcCourseRepository repository = new JdbcCourseRepository(jdbcTemplate);

        boolean exists = repository.classNameExists("软件 2301", 501L);

        assertThat(exists).isTrue();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), paramsCaptor.capture(), eq(Integer.class));
        assertThat(sqlCaptor.getValue())
                .contains("course_classes")
                .contains("class_name = :className")
                .contains("id != :excludedClassId");
        assertThat(paramsCaptor.getValue().getValue("className")).isEqualTo("软件 2301");
        assertThat(paramsCaptor.getValue().getValue("excludedClassId")).isEqualTo(501L);
    }

    @Test
    void findStudentIdsByTeacherIdUsesDirectAndAssignedClasses() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcCourseRepository repository = new JdbcCourseRepository(jdbcTemplate);
        when(jdbcTemplate.query(any(String.class), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(42L));

        List<Long> studentIds = repository.findStudentIdsByTeacherId(7L);

        assertThat(studentIds).containsExactly(42L);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("class_students")
                .contains("course_classes")
                .contains("class_courses")
                .contains("teacher_id = :teacherId");
    }

    @Test
    void replaceStudentClassDeletesOldClassAndInsertsNewClass() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcCourseRepository repository = new JdbcCourseRepository(jdbcTemplate);

        repository.replaceStudentClass(42L, 501L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(2))
                .update(sqlCaptor.capture(), any(MapSqlParameterSource.class));
        assertThat(sqlCaptor.getAllValues().get(0)).contains("DELETE FROM class_students");
        assertThat(sqlCaptor.getAllValues().get(1)).contains("INSERT IGNORE INTO class_students");
    }

    @Test
    @SuppressWarnings("unchecked")
    void teacherKnowledgePointRepositoryPersistsAndReadsLegacyCompatibleFields() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcTeacherKnowledgePointRepository repository = new JdbcTeacherKnowledgePointRepository(jdbcTemplate);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(2);
            keyHolder.getKeyList().add(Map.of("id", 11L));
            return 1;
        }).when(jdbcTemplate).update(any(String.class), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class));

        when(jdbcTemplate.query(eq("""
                SELECT kp.id AS id,
                       kp.point_name AS pointName,
                       kp.point_name AS name,
                       kp.description AS description,
                       kp.difficulty AS difficulty,
                       kp.order_index AS orderIndex,
                       kp.course_id AS courseId,
                       c.course_name AS courseName
                FROM teacher_knowledge_points kp
                JOIN courses c ON c.id = kp.course_id
                WHERE c.teacher_id = :teacherId
                  AND kp.id = :knowledgePointId
                """), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(Map.of(
                        "id", 11L,
                        "pointName", "面向对象基础",
                        "name", "面向对象基础",
                        "description", "封装、继承、多态",
                        "difficulty", "中等",
                        "orderIndex", 1,
                        "courseId", 101L,
                        "courseName", "Distributed Systems"
                )));

        Long id = repository.insert(Map.of(
                "pointName", "面向对象基础",
                "description", "封装、继承、多态",
                "difficulty", "中等",
                "orderIndex", 1,
                "courseId", 101L
        ));
        Map<String, Object> loaded = repository.findByTeacherIdAndKnowledgePointId(7L, 11L).orElseThrow();

        assertThat(id).isEqualTo(11L);
        assertThat(loaded)
                .containsEntry("id", 11L)
                .containsEntry("pointName", "面向对象基础")
                .containsEntry("name", "面向对象基础")
                .containsEntry("courseId", 101L);
    }
}
