package com._202510007517.platform.user.repository;

import com._202510007517.platform.user.domain.UserRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@Repository
public class JdbcUserRepository implements UserRepository {

    private static final RowMapper<UserRecord> USER_ROW_MAPPER = new UserRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserRecord> findById(Long id) {
        List<UserRecord> rows = jdbcTemplate.query("""
                SELECT id, username, name, email, phone, avatar, enabled, created_at, updated_at
                FROM users
                WHERE id = ?
                """, USER_ROW_MAPPER, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        List<UserRecord> rows = jdbcTemplate.query("""
                SELECT id, username, name, email, phone, avatar, enabled, created_at, updated_at
                FROM users
                WHERE username = ?
                """, USER_ROW_MAPPER, username);
        return rows.stream().findFirst();
    }

    @Override
    public List<UserRecord> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        StringJoiner placeholders = new StringJoiner(",", "(", ")");
        ids.forEach(id -> placeholders.add("?"));
        String sql = """
                SELECT id, username, name, email, phone, avatar, enabled, created_at, updated_at
                FROM users
                WHERE id IN %s
                """.formatted(placeholders);
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, ids.toArray());
    }

    @Override
    public List<String> findRolesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT r.name
                FROM roles r
                JOIN user_roles ur ON r.id = ur.role_id
                WHERE ur.user_id = ?
                ORDER BY r.id
                """, String.class, userId);
    }

    @Override
    public String findStudentClassName(Long studentId) {
        // Class ownership belongs to Course_Service. Keep the field stable in
        // stage 2 and enrich it through Course_Service after that split lands.
        return "未知班级";
    }

    @Override
    public void updateProfile(UserRecord user) {
        jdbcTemplate.update("""
                UPDATE users
                SET name = ?, email = ?, phone = ?, avatar = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, user.getName(), user.getEmail(), user.getPhone(), user.getAvatar(), user.isEnabled(), user.getId());
    }

    private static class UserRowMapper implements RowMapper<UserRecord> {
        @Override
        public UserRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRecord user = new UserRecord();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setAvatar(rs.getString("avatar"));
            user.setEnabled(rs.getBoolean("enabled"));
            user.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            user.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
            return user;
        }
    }
}
