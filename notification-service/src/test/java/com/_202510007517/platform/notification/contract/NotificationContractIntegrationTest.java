package com._202510007517.platform.notification.contract;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.JdbcProcessedEventRepository;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedPayload;
import com._202510007517.platform.notification.repository.JdbcNotificationRepository;
import com._202510007517.platform.notification.repository.NotificationRepository;
import com._202510007517.platform.notification.service.AssignmentSubmittedNotificationHandler;
import com._202510007517.platform.notification.service.EarlyWarningRaisedNotificationHandler;
import com._202510007517.platform.notification.service.ExamFinishedNotificationHandler;
import com._202510007517.platform.notification.service.NotificationCommandService;
import com._202510007517.platform.notification.service.NotificationQueryService;
import com._202510007517.platform.notification.controller.NotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationContractIntegrationTest {

    private JdbcTemplate jdbcTemplate;
    private MockMvc mockMvc;
    private AssignmentSubmittedNotificationHandler assignmentSubmittedHandler;
    private ExamFinishedNotificationHandler examFinishedHandler;
    private EarlyWarningRaisedNotificationHandler earlyWarningRaisedHandler;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notification-contract;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchema();

        NotificationRepository notificationRepository = new JdbcNotificationRepository(
                new NamedParameterJdbcTemplate(dataSource));
        IdempotentEventHandler idempotentEventHandler = new IdempotentEventHandler(
                new JdbcProcessedEventRepository(jdbcTemplate));
        NotificationController controller = new NotificationController(
                new NotificationQueryService(notificationRepository),
                new NotificationCommandService(notificationRepository));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        assignmentSubmittedHandler = new AssignmentSubmittedNotificationHandler(idempotentEventHandler, notificationRepository);
        examFinishedHandler = new ExamFinishedNotificationHandler(idempotentEventHandler, notificationRepository);
        earlyWarningRaisedHandler = new EarlyWarningRaisedNotificationHandler(idempotentEventHandler, notificationRepository);
    }

    @Test
    void httpContractReadsAndMutatesStudentNotifications() throws Exception {
        Long unreadId = insertNotification(42L, null, "assignment", "作业提交成功",
                "您提交的作业已收到，请等待教师批改", 2001L, false, "2026-05-19 10:20:30");
        Long readId = insertNotification(42L, 7L, "course", "开课通知",
                "请同学按时参加第一节课", 5001L, true, "2026-05-19 09:00:00");
        insertNotification(99L, null, "warning", "其他学生预警",
                "这条不应出现在 42 的结果里", 7001L, false, "2026-05-19 11:00:00");

        mockMvc.perform(get("/api/notifications/student")
                        .header("X-User-Id", "42")
                        .param("page", "1")
                        .param("size", "10")
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.notifications.length()").value(2))
                .andExpect(jsonPath("$.data.notifications[0].studentId").value(42))
                .andExpect(jsonPath("$.data.notifications[0].title").value("作业提交成功"))
                .andExpect(jsonPath("$.data.notifications[0].read").value(false))
                .andExpect(jsonPath("$.data.notifications[1].title").value("开课通知"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        mockMvc.perform(get("/api/notifications/student")
                        .header("X-User-Id", "42")
                        .param("page", "1")
                        .param("size", "1")
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/notifications/student/all")
                        .header("X-User-Id", "42")
                        .param("filter", "unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(unreadId))
                .andExpect(jsonPath("$.data[0].read").value(false));

        mockMvc.perform(get("/api/notifications/student/unread-count")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(put("/api/notifications/{notificationId}/read", unreadId)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(readFlag(unreadId)).isTrue();

        mockMvc.perform(delete("/api/notifications/{notificationId}", readId)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE id = ?", readId)).isZero();

        mockMvc.perform(post("/api/notifications/teacher/send")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "course",
                                  "title": "补课通知",
                                  "content": "周五晚自习补课",
                                  "studentId": 42,
                                  "relatedId": 5001,
                                  "isRead": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.teacherId").value(7))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.title").value("补课通知"))
                .andExpect(jsonPath("$.data.read").value(false));

        mockMvc.perform(post("/api/notifications/teacher/send-batch")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                [
                                  {
                                    "type": "course",
                                    "title": "批量通知",
                                    "content": "第一位同学通知",
                                    "studentId": 42,
                                    "relatedId": 5002,
                                    "isRead": false
                                  },
                                  {
                                    "type": "course",
                                    "title": "批量通知",
                                    "content": "第二位同学通知",
                                    "studentId": 43,
                                    "relatedId": 5002,
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
                .andExpect(jsonPath("$.data[1].teacherId").value(7))
                .andExpect(jsonPath("$.data[1].studentId").value(43));
        assertThat(count("SELECT COUNT(*) FROM notifications WHERE teacher_id = ? AND title = ?", 7L, "批量通知"))
                .isEqualTo(2);
    }

    @Test
    void eventContractPersistsNotificationsAndProcessedEventsIdempotently() {
        assignmentSubmittedHandler.handle(new AssignmentSubmittedEvent(
                "notification-contract-assignment-submitted",
                Instant.parse("2026-05-19T10:15:30Z"),
                new EventAggregate("assignment_submission", "3001"),
                new AssignmentSubmittedPayload(
                        2001L,
                        3001L,
                        42L,
                        5001L,
                        6001L,
                        false,
                        Instant.parse("2026-05-19T10:15:00Z"))));
        examFinishedHandler.handle(new ExamFinishedEvent(
                "notification-contract-exam-finished",
                Instant.parse("2026-05-19T11:00:00Z"),
                new EventAggregate("exam_submission", "9001"),
                new ExamFinishedPayload(
                        77L,
                        9001L,
                        42L,
                        5001L,
                        6001L,
                        85,
                        100,
                        Instant.parse("2026-05-19T10:59:00Z"))));
        EarlyWarningRaisedEvent warningEvent = new EarlyWarningRaisedEvent(
                "notification-contract-warning-raised",
                Instant.parse("2026-05-19T11:10:00Z"),
                new EventAggregate("early_warning", "7001"),
                new EarlyWarningRaisedPayload(
                        7001L,
                        42L,
                        5001L,
                        "LOW_SCORE",
                        "HIGH",
                        "学情预警",
                        "考试得分率 45%，低于预警阈值 60%"));
        earlyWarningRaisedHandler.handle(warningEvent);
        earlyWarningRaisedHandler.handle(warningEvent);

        assertThat(count("SELECT COUNT(*) FROM notifications")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM processed_event")).isEqualTo(3);

        Map<String, Object> assignment = jdbcTemplate.queryForMap("""
                SELECT student_id, type, title, content, related_id, is_read
                FROM notifications
                WHERE type = 'assignment'
                ORDER BY id
                LIMIT 1
                """);
        assertThat(assignment.get("student_id")).isEqualTo(42L);
        assertThat(assignment.get("title")).isEqualTo("作业提交成功");
        assertThat((String) assignment.get("content")).contains("已收到");
        assertThat(assignment.get("related_id")).isEqualTo(2001L);
        assertThat(assignment.get("is_read")).isEqualTo(false);

        Map<String, Object> exam = jdbcTemplate.queryForMap("""
                SELECT student_id, type, title, content, related_id
                FROM notifications
                WHERE type = 'exam'
                """);
        assertThat(exam.get("student_id")).isEqualTo(42L);
        assertThat(exam.get("title")).isEqualTo("考试已完成");
        assertThat((String) exam.get("content")).contains("85/100");
        assertThat(exam.get("related_id")).isEqualTo(77L);

        Map<String, Object> warning = jdbcTemplate.queryForMap("""
                SELECT student_id, type, title, content, related_id
                FROM notifications
                WHERE type = 'warning'
                """);
        assertThat(warning.get("student_id")).isEqualTo(42L);
        assertThat(warning.get("title")).isEqualTo("学情预警");
        assertThat((String) warning.get("content")).contains("45%");
        assertThat(warning.get("related_id")).isEqualTo(7001L);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS processed_event");
        jdbcTemplate.execute("DROP TABLE IF EXISTS notifications");
        jdbcTemplate.execute("""
                CREATE TABLE notifications (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    teacher_id BIGINT NULL,
                    type VARCHAR(50) NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    related_id BIGINT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE processed_event (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    event_id VARCHAR(100) NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    consumer_name VARCHAR(100) NOT NULL,
                    processed_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT uk_processed_event_event_consumer UNIQUE (event_id, consumer_name)
                )
                """);
    }

    private Long insertNotification(Long studentId,
                                    Long teacherId,
                                    String type,
                                    String title,
                                    String content,
                                    Long relatedId,
                                    boolean read,
                                    String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO notifications (
                    student_id, teacher_id, type, title, content, related_id, is_read, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, studentId, teacherId, type, title, content, relatedId, read, createdAt);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM notifications", Long.class);
    }

    private boolean readFlag(Long notificationId) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT is_read FROM notifications WHERE id = ?",
                Boolean.class,
                notificationId);
        return Boolean.TRUE.equals(value);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
