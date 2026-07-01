package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.mapper.NotificationMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    @Test
    void getNotificationsWithPagination_boundsOutOfRangePageAndPreservesLegacyShape() {
        NotificationMapper mapper = mock(NotificationMapper.class);

        NotificationServiceImpl service = new NotificationServiceImpl();
        ReflectionTestUtils.setField(service, "notificationMapper", mapper);

        Notification unread = buildNotification(1001L, "system", false, "系统通知");
        Notification read = buildNotification(1002L, "system", true, "系统通知-已读");
        Notification latestUnread = buildNotification(1003L, "system", false, "系统通知-最新");

        when(mapper.getNotificationsByStudentId(7L)).thenReturn(List.of(unread, read, latestUnread));

        Map<String, Object> result = service.getNotificationsWithPagination(7L, 9, 2, "all");

        assertThat(result.get("page")).isEqualTo(2);
        assertThat(result.get("size")).isEqualTo(2);
        assertThat(result.get("total")).isEqualTo(3);
        assertThat(result.get("totalPages")).isEqualTo(2);
        assertThat(result.get("notifications")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .extracting(item -> ((Notification) item).getId())
                .containsExactly(1003L);
    }

    @Test
    void getNotificationsWithPagination_returnsFirstPageForEmptyResultSet() {
        NotificationMapper mapper = mock(NotificationMapper.class);

        NotificationServiceImpl service = new NotificationServiceImpl();
        ReflectionTestUtils.setField(service, "notificationMapper", mapper);

        when(mapper.getNotificationsByStudentId(7L)).thenReturn(List.of());

        Map<String, Object> result = service.getNotificationsWithPagination(7L, 5, 10, "all");

        assertThat(result.get("page")).isEqualTo(1);
        assertThat(result.get("size")).isEqualTo(10);
        assertThat(result.get("total")).isEqualTo(0);
        assertThat(result.get("totalPages")).isEqualTo(0);
        assertThat(result.get("notifications")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
    }

    private Notification buildNotification(Long id, String type, boolean isRead, String title) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType(type);
        notification.setIsRead(isRead);
        notification.setTitle(title);
        notification.setContent(title + "内容");
        return notification;
    }
}
