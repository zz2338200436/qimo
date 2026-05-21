package com._202510007517.platform.assignment.service;

import com._202510007517.platform.common.event.outbox.OutboxRelayJob;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AssignmentOutboxAdminService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<OutboxRelayJob> outboxRelayJobProvider;

    public AssignmentOutboxAdminService(JdbcTemplate jdbcTemplate,
                                        ObjectProvider<OutboxRelayJob> outboxRelayJobProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboxRelayJobProvider = outboxRelayJobProvider;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("relayJobPresent", outboxRelayJobProvider.getIfAvailable() != null);
        status.put("pendingCount", countByStatus(0));
        status.put("publishedCount", countByStatus(1));
        status.put("failedCount", countByStatus(2));
        return status;
    }

    public int relayOnce() {
        OutboxRelayJob relayJob = outboxRelayJobProvider.getIfAvailable();
        if (relayJob == null) {
            throw new IllegalStateException("Outbox relay job is not available");
        }
        return relayJob.relayOnce();
    }

    private long countByStatus(int status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = ?",
                Long.class,
                status);
        return count == null ? 0L : count;
    }
}
