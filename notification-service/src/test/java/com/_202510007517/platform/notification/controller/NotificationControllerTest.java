package com._202510007517.platform.notification.controller;

import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.service.NotificationCommandService;
import com._202510007517.platform.notification.service.NotificationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    @Test
    void listStudentNotificationsReturnsPagedEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        when(queryService.getStudentNotifications(42L, 1, 10, "all")).thenReturn(Map.of(
                "notifications", List.of(notification(1L, 42L, "assignment", "作业提交成功")),
                "total", 1L,
                "page", 1,
                "size", 10
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(get("/api/notifications/student")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications[0].id").value(1))
                .andExpect(jsonPath("$.data.notifications[0].title").value("作业提交成功"))
                .andExpect(jsonPath("$.data.notifications[0].read").value(false))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listAllStudentNotificationsReturnsEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        when(queryService.getAllStudentNotifications(42L, "all")).thenReturn(List.of(
                notification(1L, 42L, "assignment", "作业提交成功")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(get("/api/notifications/student/all")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].studentId").value(42));
    }

    @Test
    void getUnreadCountReturnsEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        when(queryService.getUnreadCount(42L)).thenReturn(3);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(get("/api/notifications/student/unread-count")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void listTeacherSentNotificationsReturnsPagedEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        NotificationEntity sent = notification(12L, 42L, "course", "开课通知");
        sent.setTeacherId(7L);
        when(queryService.getTeacherSentNotifications(7L, 1, 10, "all")).thenReturn(Map.of(
                "notifications", List.of(sent),
                "total", 1L,
                "page", 1,
                "size", 10,
                "totalPages", 1
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(get("/api/notifications/teacher/sent")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications[0].id").value(12))
                .andExpect(jsonPath("$.data.notifications[0].teacherId").value(7))
                .andExpect(jsonPath("$.data.notifications[0].studentId").value(42))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void markNotificationAsReadReturnsSuccessEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(put("/api/notifications/9/read")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void markAllNotificationsAsReadReturnsSuccessEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteNotificationReturnsSuccessEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(delete("/api/notifications/9")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteAllReadNotificationsReturnsSuccessEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(delete("/api/notifications/delete-all-read")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void sendTeacherNotificationReturnsCreatedEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        NotificationEntity created = notification(12L, 42L, "course", "开课通知");
        created.setTeacherId(7L);
        when(commandService.sendNotification(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(TeacherSendNotificationRequestDTO.class)))
                .thenReturn(created);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(post("/api/notifications/teacher/send")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "course",
                                  "title": "开课通知",
                                  "content": "请同学按时参加第一节课",
                                  "studentId": 42,
                                  "relatedId": null,
                                  "isRead": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.teacherId").value(7))
                .andExpect(jsonPath("$.data.studentId").value(42));
    }

    @Test
    void sendTeacherBatchNotificationsReturnsCreatedEnvelope() throws Exception {
        NotificationQueryService queryService = mock(NotificationQueryService.class);
        NotificationCommandService commandService = mock(NotificationCommandService.class);
        NotificationEntity first = notification(12L, 42L, "course", "开课通知");
        first.setTeacherId(7L);
        NotificationEntity second = notification(13L, 43L, "course", "开课通知");
        second.setTeacherId(7L);
        when(commandService.sendBatchNotifications(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(first, second));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(queryService, commandService)).build();

        mockMvc.perform(post("/api/notifications/teacher/send-batch")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                [
                                  {
                                    "type": "course",
                                    "title": "开课通知",
                                    "content": "请同学按时参加第一节课",
                                    "studentId": 42,
                                    "relatedId": 5001,
                                    "isRead": false
                                  },
                                  {
                                    "type": "course",
                                    "title": "开课通知",
                                    "content": "请同学按时参加第一节课",
                                    "studentId": 43,
                                    "relatedId": 5001,
                                    "isRead": false
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].teacherId").value(7))
                .andExpect(jsonPath("$.data[0].studentId").value(42))
                .andExpect(jsonPath("$.data[1].studentId").value(43));
    }

    private NotificationEntity notification(Long id, Long studentId, String type, String title) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(id);
        notification.setStudentId(studentId);
        notification.setTeacherId(null);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent("您提交的作业已收到，请等待教师批改");
        notification.setRelatedId(2001L);
        notification.setRead(false);
        notification.setCreatedAt(Instant.parse("2026-05-15T10:20:30Z"));
        return notification;
    }
}
