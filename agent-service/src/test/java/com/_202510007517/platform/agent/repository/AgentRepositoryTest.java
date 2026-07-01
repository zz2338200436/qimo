package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentAuditLogEntity;
import com._202510007517.platform.agent.domain.AgentMessageEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:classpath:application-common.yml",
                "spring.datasource.url=jdbc:h2:mem:agent-repo;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
@ActiveProfiles("test")
class AgentRepositoryTest {

    @Autowired
    private AgentSessionRepository sessionRepository;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesPendingActionWithIdempotencyKey() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"作业\"}");
        action.setRequestJson("{\"title\":\"作业\"}");
        action.setIdempotencyKey("idem-1");
        AgentActionEntity savedAction = actionRepository.save(action);

        assertThat(actionRepository.findById(savedAction.getId())).isPresent();
        assertThat(savedAction.getIdempotencyKey()).isEqualTo("idem-1");
    }

    @Test
    void savesPendingSessionContextForMultiTurnCompletion() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        session.setPendingIntent("PUBLISH_ASSIGNMENT");
        session.setPendingSlotsJson("{\"title\":\"微服务实验\"}");

        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentSessionEntity reloaded = sessionRepository.findById(savedSession.getId()).orElseThrow();
        assertThat(reloaded.getPendingIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(reloaded.getPendingSlotsJson()).contains("微服务实验");
    }

    @Test
    void savesMessagesAndReturnsSessionHistoryInStableOrder() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("STUDENT");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        jdbcTemplate.update("""
                INSERT INTO agent_messages (session_id, role, content, metadata_json, created_at)
                VALUES (?, ?, ?, ?, TIMESTAMP '2026-06-13 12:00:00.000000')
                """, savedSession.getId(), "USER", "今天有什么作业？", null);
        Long userMessageId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM agent_messages", Long.class);
        jdbcTemplate.update("""
                INSERT INTO agent_messages (session_id, role, content, metadata_json, created_at)
                VALUES (?, ?, ?, ?, TIMESTAMP '2026-06-13 12:00:00.000000')
                """, savedSession.getId(), "ASSISTANT", "查询完成。", "{\"responseType\":\"DATA\"}");
        Long assistantMessageId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM agent_messages", Long.class);

        var messages = messageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(savedSession.getId());

        assertThat(messages).extracting(AgentMessageEntity::getId)
                .containsExactly(userMessageId, assistantMessageId);
        assertThat(messages).extracting(AgentMessageEntity::getRole)
                .containsExactly("USER", "ASSISTANT");
        assertThat(messages.get(1).getMetadataJson()).contains("DATA");
    }

    @Test
    void savesAuditLogForActionExecution() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(42L);
        session.setUserRole("STUDENT");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("SUBMIT_ASSIGNMENT");
        action.setStatus("EXECUTED");
        action.setRiskLevel("MEDIUM");
        action.setIdempotencyKey("idem-2");
        AgentActionEntity savedAction = actionRepository.save(action);

        AgentAuditLogEntity audit = new AgentAuditLogEntity();
        audit.setActionId(savedAction.getId());
        audit.setUserId(42L);
        audit.setUserRole("STUDENT");
        audit.setOperation("SUBMIT_ASSIGNMENT");
        audit.setTargetService("assignment-service");
        audit.setTargetResource("assignment:100");
        audit.setTraceId("trace-1");
        audit.setSuccess(true);
        AgentAuditLogEntity savedAudit = auditLogRepository.save(audit);

        assertThat(auditLogRepository.findById(savedAudit.getId())).isPresent();
        assertThat(savedAudit.getTargetService()).isEqualTo("assignment-service");
    }
}
