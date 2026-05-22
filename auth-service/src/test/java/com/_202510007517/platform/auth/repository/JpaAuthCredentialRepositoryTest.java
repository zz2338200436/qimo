package com._202510007517.platform.auth.repository;

import com._202510007517.platform.auth.AuthServiceApplication;
import com._202510007517.platform.auth.domain.AuthCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AuthServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaAuthCredentialRepositoryTest {

    private static final String DATABASE_NAME = "auth-jpa-" + UUID.randomUUID();

    @Autowired
    private AuthCredentialRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.stream.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @BeforeEach
    void setUp() {
        recreateSchema();
        seedData();
    }

    @Test
    void findsCredentialsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAuthCredentialRepository.class);

        Optional<AuthCredential> byUsername = repository.findByUsername("alice");
        Optional<AuthCredential> byUserId = repository.findByUserId(42L);
        Optional<AuthCredential> missing = repository.findByUsername("missing");

        assertThat(byUsername).isPresent();
        assertThat(byUsername.orElseThrow().getUserId()).isEqualTo(42L);
        assertThat(byUsername.orElseThrow().getPasswordHash()).isEqualTo("{bcrypt}alice-hash");
        assertThat(byUsername.orElseThrow().isEnabled()).isTrue();
        assertThat(byUserId).isPresent();
        assertThat(byUserId.orElseThrow().getUsername()).isEqualTo("alice");
        assertThat(missing).isEmpty();
    }

    @Test
    void updatesPasswordThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAuthCredentialRepository.class);

        repository.updatePassword(43L, "{bcrypt}new-bob-hash");

        Optional<AuthCredential> updated = repository.findByUserId(43L);
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM auth_credentials WHERE user_id = ?",
                String.class,
                43L);

        assertThat(updated).isPresent();
        assertThat(updated.orElseThrow().getPasswordHash()).isEqualTo("{bcrypt}new-bob-hash");
        assertThat(storedHash).isEqualTo("{bcrypt}new-bob-hash");
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_credentials");
        jdbcTemplate.execute("""
                CREATE TABLE auth_credentials (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    password_hash VARCHAR(100) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE
                )
                """);
    }

    private void seedData() {
        jdbcTemplate.update("""
                INSERT INTO auth_credentials (id, user_id, username, password_hash, enabled)
                VALUES (?, ?, ?, ?, ?)
                """,
                1L, 42L, "alice", "{bcrypt}alice-hash", true);
        jdbcTemplate.update("""
                INSERT INTO auth_credentials (id, user_id, username, password_hash, enabled)
                VALUES (?, ?, ?, ?, ?)
                """,
                2L, 43L, "bob", "{bcrypt}bob-hash", false);
    }
}
