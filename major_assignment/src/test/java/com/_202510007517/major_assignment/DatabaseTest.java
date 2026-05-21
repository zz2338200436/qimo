package com._202510007517.major_assignment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class DatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".*")
    public void testDatabaseConnection() {
        String sql = "SELECT COUNT(*) FROM users";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        System.out.println("Number of users: " + count);
        assert count != null && count > 0;
    }
}