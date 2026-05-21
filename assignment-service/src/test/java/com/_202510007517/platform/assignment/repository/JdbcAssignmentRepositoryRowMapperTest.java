package com._202510007517.platform.assignment.repository;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcAssignmentRepositoryRowMapperTest {

    @Test
    void assignmentRowMapperAcceptsLongBackedNumericColumns() throws Exception {
        @SuppressWarnings("unchecked")
        RowMapper<AssignmentRecord> mapper = (RowMapper<AssignmentRecord>) instantiateMapper(
                "com._202510007517.platform.assignment.repository.JdbcAssignmentRepository$AssignmentRowMapper");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("title")).thenReturn("AssignmentSmokeA");
        when(rs.getString("description")).thenReturn("smoke");
        when(rs.getObject("course_id")).thenReturn(2L);
        when(rs.getString("due_date")).thenReturn("2099-12-31 23:59:59");
        when(rs.getString("publish_date")).thenReturn("2026-05-15 00:00:00");
        when(rs.getObject("is_active")).thenReturn(Boolean.TRUE);
        when(rs.getObject("teacher_id")).thenReturn(7L);
        when(rs.getObject("max_score")).thenReturn(100L);
        when(rs.getObject("submission_count")).thenReturn(1L);
        when(rs.getObject("graded_count")).thenReturn(0L);
        when(rs.getString("status")).thenReturn("submitted");
        when(rs.getObject("total_students")).thenReturn(1L);

        AssignmentRecord record = mapper.mapRow(rs, 1);

        assertThat(record.getMaxScore()).isEqualTo(100);
        assertThat(record.getSubmissionCount()).isEqualTo(1);
        assertThat(record.getGradedCount()).isZero();
        assertThat(record.getTotalStudents()).isEqualTo(1);
    }

    @Test
    void submissionRowMapperAcceptsLongBackedNumericColumns() throws Exception {
        @SuppressWarnings("unchecked")
        RowMapper<AssignmentSubmissionRecord> mapper = (RowMapper<AssignmentSubmissionRecord>) instantiateMapper(
                "com._202510007517.platform.assignment.repository.JdbcAssignmentRepository$AssignmentSubmissionRowMapper");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(10L);
        when(rs.getLong("assignment_id")).thenReturn(1L);
        when(rs.getObject("student_id")).thenReturn(42L);
        when(rs.getString("content")).thenReturn("smoke answer");
        when(rs.getString("submission_date")).thenReturn("2026-05-15 00:00:00");
        when(rs.getObject("graded")).thenReturn(Boolean.TRUE);
        when(rs.getObject("is_late")).thenReturn(Boolean.FALSE);
        when(rs.getObject("late_penalty")).thenReturn(0L);
        when(rs.getObject("score")).thenReturn(96L);
        when(rs.getString("teacher_comment")).thenReturn("graded");

        AssignmentSubmissionRecord record = mapper.mapRow(rs, 1);

        assertThat(record.getLatePenalty()).isZero();
        assertThat(record.getScore()).isEqualTo(96);
    }

    @Test
    void updateAssignmentPersistsGradedCountAlongsideSubmissionCount() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcAssignmentRepository repository = new JdbcAssignmentRepository(jdbcTemplate);
        AssignmentRecord assignment = new AssignmentRecord();
        assignment.setId(1L);
        assignment.setSubmissionCount(1);
        assignment.setGradedCount(1);
        assignment.setStatus("graded");

        repository.updateAssignment(assignment);

        verify(jdbcTemplate).update(eq("""
                UPDATE assignments
                SET submission_count = :submissionCount,
                    graded_count = :gradedCount,
                    status = :status
                WHERE id = :id
                """), argThat((MapSqlParameterSource params) ->
                params != null
                        && Long.valueOf(1L).equals(params.getValue("id"))
                        && Integer.valueOf(1).equals(params.getValue("submissionCount"))
                        && Integer.valueOf(1).equals(params.getValue("gradedCount"))
                        && "graded".equals(params.getValue("status"))));
    }

    private static Object instantiateMapper(String className) throws Exception {
        Class<?> mapperClass = Class.forName(className);
        Constructor<?> constructor = mapperClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
