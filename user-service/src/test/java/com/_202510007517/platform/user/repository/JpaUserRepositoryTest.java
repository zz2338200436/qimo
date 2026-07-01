package com._202510007517.platform.user.repository;

import com._202510007517.platform.user.UserServiceApplication;
import com._202510007517.platform.user.domain.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = UserServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaUserRepositoryTest {

    private static final String DATABASE_NAME = "user-jpa-" + UUID.randomUUID();

    @Autowired
    private UserRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void cleanUsers() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void findsUsersAndOrderedRolesThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaUserRepository.class);
        insertUser(42L, "student42", "学生四二");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", 42L, 3L);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", 42L, 1L);

        Optional<UserRecord> user = repository.findByUsername("student42");

        assertThat(user).isPresent();
        assertThat(user.orElseThrow().getId()).isEqualTo(42L);
        assertThat(user.orElseThrow().getName()).isEqualTo("学生四二");
        assertThat(repository.findById(42L)).isPresent();
        assertThat(repository.findByIds(List.of(42L, 99L)))
                .extracting(UserRecord::getUsername)
                .containsExactly("student42");
        assertThat(repository.findByIds(List.of())).isEmpty();
        assertThat(repository.findRolesByUserId(42L)).containsExactly("ADMIN", "STUDENT");
        assertThat(repository.findStudentClassName(42L)).isEqualTo("未知班级");
    }

    @Test
    void updateProfilePersistsMutableUserFieldsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaUserRepository.class);
        insertUser(42L, "student42", "学生四二");

        UserRecord user = repository.findById(42L).orElseThrow();
        user.setName("学生新名");
        user.setEmail("new42@example.com");
        user.setPhone("13900000042");
        user.setAvatar("/avatars/new.png");
        user.setEnabled(false);

        repository.updateProfile(user);

        UserRecord updated = repository.findById(42L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("学生新名");
        assertThat(updated.getEmail()).isEqualTo("new42@example.com");
        assertThat(updated.getPhone()).isEqualTo("13900000042");
        assertThat(updated.getAvatar()).isEqualTo("/avatars/new.png");
        assertThat(updated.isEnabled()).isFalse();
    }

    private void insertUser(Long id, String username, String name) {
        jdbcTemplate.update("""
                INSERT INTO users (id, username, name, email, phone, avatar, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                username,
                name,
                username + "@example.com",
                "13800000042",
                "/avatars/42.png",
                true);
    }
}
