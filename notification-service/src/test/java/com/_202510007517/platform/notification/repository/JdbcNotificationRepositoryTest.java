package com._202510007517.platform.notification.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcNotificationRepositoryTest {

    @Test
    void mapRowBuildsNotificationEntityFromJdbcValues() throws Exception {
        JdbcNotificationRepository repository = new JdbcNotificationRepository(null);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("student_id")).thenReturn(42L);
        when(resultSet.getObject("teacher_id")).thenReturn(null);
        when(resultSet.getString("type")).thenReturn("assignment");
        when(resultSet.getString("title")).thenReturn("作业提交成功");
        when(resultSet.getString("content")).thenReturn("您提交的作业已收到，请等待教师批改");
        when(resultSet.getLong("related_id")).thenReturn(2001L);
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getBoolean("is_read")).thenReturn(false);
        when(resultSet.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.from(
                Instant.parse("2026-05-15T10:30:00Z")));

        Method mapper = JdbcNotificationRepository.class.getDeclaredMethod("mapRow", ResultSet.class);
        mapper.setAccessible(true);
        NotificationEntity notification = (NotificationEntity) mapper.invoke(repository, resultSet);

        assertThat(notification.getId()).isEqualTo(1L);
        assertThat(notification.getStudentId()).isEqualTo(42L);
        assertThat(notification.getTeacherId()).isNull();
        assertThat(notification.getType()).isEqualTo("assignment");
        assertThat(notification.getTitle()).isEqualTo("作业提交成功");
        assertThat(notification.getContent()).contains("已收到");
        assertThat(notification.getRelatedId()).isEqualTo(2001L);
        assertThat(notification.getRead()).isFalse();
    }

    @Test
    void normalizeFilterFallsBackToAllAndLowercase() throws Exception {
        JdbcNotificationRepository repository = new JdbcNotificationRepository(null);
        Method normalizeFilter = JdbcNotificationRepository.class.getDeclaredMethod("normalizeFilter", String.class);
        normalizeFilter.setAccessible(true);

        assertThat(normalizeFilter.invoke(repository, new Object[]{null})).isEqualTo("all");
        assertThat(normalizeFilter.invoke(repository, "  UNREAD ")).isEqualTo("unread");
        assertThat(normalizeFilter.invoke(repository, "course")).isEqualTo("course");
    }
}
