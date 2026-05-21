package com._202510007517.platform.auth.repository;

import com._202510007517.platform.auth.domain.AuthCredential;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcAuthCredentialRepository implements AuthCredentialRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AuthCredential> findByUsername(String username) {
        String sql = """
                SELECT user_id, username, password_hash, enabled
                FROM auth_credentials
                WHERE username = ?
                """;
        return queryOne(sql, username);
    }

    @Override
    public Optional<AuthCredential> findByUserId(Long userId) {
        String sql = """
                SELECT user_id, username, password_hash, enabled
                FROM auth_credentials
                WHERE user_id = ?
                """;
        return queryOne(sql, userId);
    }

    private Optional<AuthCredential> queryOne(String sql, Object arg) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            AuthCredential credential = new AuthCredential();
            credential.setUserId(rs.getLong("user_id"));
            credential.setUsername(rs.getString("username"));
            credential.setPasswordHash(rs.getString("password_hash"));
            credential.setEnabled(rs.getBoolean("enabled"));
            return Optional.of(credential);
        }, arg);
    }

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        jdbcTemplate.update("UPDATE auth_credentials SET password_hash = ? WHERE user_id = ?", encodedPassword, userId);
    }
}
