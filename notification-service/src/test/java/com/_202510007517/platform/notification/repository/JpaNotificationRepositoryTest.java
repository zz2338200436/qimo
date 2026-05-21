package com._202510007517.platform.notification.repository;

import com._202510007517.platform.notification.NotificationServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NotificationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaNotificationRepositoryTest {

    private static final String DATABASE_NAME = "notification-jpa-" + UUID.randomUUID();

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.stream.bindings.assignmentSubmittedConsumer-in-0.consumer.auto-startup", () -> "false");
        registry.add("spring.cloud.stream.bindings.assignmentGradedConsumer-in-0.consumer.auto-startup", () -> "false");
        registry.add("spring.cloud.stream.bindings.examFinishedConsumer-in-0.consumer.auto-startup", () -> "false");
        registry.add("spring.cloud.stream.bindings.earlyWarningRaisedConsumer-in-0.consumer.auto-startup", () -> "false");
        registry.add("spring.cloud.stream.bindings.assignmentSubmittedConsumer-in-0.destination", () -> "disabled.assignment");
        registry.add("spring.cloud.stream.bindings.assignmentGradedConsumer-in-0.destination", () -> "disabled.assignment.graded");
        registry.add("spring.cloud.stream.bindings.examFinishedConsumer-in-0.destination", () -> "disabled.exam");
        registry.add("spring.cloud.stream.bindings.earlyWarningRaisedConsumer-in-0.destination", () -> "disabled.warning");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void cleanNotifications() {
        recreateSchema();
    }

    @Test
    void savesAndQueriesStudentNotificationsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaNotificationRepository.class);
        NotificationEntity unread = saveNotification(42L, null, "assignment", "作业提交成功", 2001L, false,
                Instant.parse("2026-05-19T10:20:30Z"));
        NotificationEntity read = saveNotification(42L, 7L, "course", "开课通知", 5001L, true,
                Instant.parse("2026-05-19T09:00:00Z"));
        saveNotification(99L, null, "warning", "其他学生预警", 7001L, false,
                Instant.parse("2026-05-19T11:00:00Z"));

        List<NotificationEntity> page = repository.findStudentNotifications(42L, 0, 10, "all");

        assertThat(page)
                .extracting(NotificationEntity::getId)
                .containsExactly(unread.getId(), read.getId());
        assertThat(repository.countStudentNotifications(42L, "all")).isEqualTo(2);
        assertThat(repository.findAllStudentNotifications(42L, "unread"))
                .extracting(NotificationEntity::getId)
                .containsExactly(unread.getId());
        assertThat(repository.findAllStudentNotifications(42L, "COURSE"))
                .extracting(NotificationEntity::getId)
                .containsExactly(read.getId());
        assertThat(repository.findStudentNotifications(42L, 1, 1, "all"))
                .extracting(NotificationEntity::getId)
                .containsExactly(read.getId());
        assertThat(repository.countUnreadByStudentId(42L)).isEqualTo(1);
    }

    @Test
    void mutatesNotificationsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaNotificationRepository.class);
        NotificationEntity unread = saveNotification(42L, null, "assignment", "作业提交成功", 2001L, false,
                Instant.parse("2026-05-19T10:20:30Z"));
        NotificationEntity read = saveNotification(42L, 7L, "course", "开课通知", 5001L, true,
                Instant.parse("2026-05-19T09:00:00Z"));
        saveNotification(99L, null, "warning", "其他学生预警", 7001L, false,
                Instant.parse("2026-05-19T11:00:00Z"));

        assertThat(repository.markAsRead(42L, unread.getId())).isEqualTo(1);
        assertThat(repository.markAsRead(99L, read.getId())).isZero();
        assertThat(repository.countUnreadByStudentId(42L)).isZero();

        assertThat(repository.markAllAsRead(99L)).isEqualTo(1);
        assertThat(repository.deleteAllReadByStudentId(42L)).isEqualTo(2);
        assertThat(repository.findAllStudentNotifications(42L, "all")).isEmpty();
        assertThat(repository.deleteByIdAndStudentId(99L, read.getId())).isZero();
    }

    @Test
    void saveDefaultsNullReadAndCreatedAtLikePreviousJdbcInsert() {
        assertThat(repository).isInstanceOf(JpaNotificationRepository.class);
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(42L);
        notification.setTeacherId(7L);
        notification.setType("course");
        notification.setTitle("开课通知");
        notification.setContent("请同学按时参加第一节课");
        notification.setRelatedId(5001L);

        NotificationEntity saved = repository.save(notification);

        NotificationEntity reloaded = repository.findAllStudentNotifications(42L, "all").get(0);
        assertThat(saved.getId()).isNotNull();
        assertThat(reloaded.getRead()).isFalse();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    private NotificationEntity saveNotification(Long studentId,
                                                Long teacherId,
                                                String type,
                                                String title,
                                                Long relatedId,
                                                boolean read,
                                                Instant createdAt) {
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(studentId);
        notification.setTeacherId(teacherId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(title + "内容");
        notification.setRelatedId(relatedId);
        notification.setRead(read);
        notification.setCreatedAt(createdAt);
        return repository.save(notification);
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
}
