package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-slot-validation;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
class AgentSlotValidationOrchestratorTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @MockitoBean
    private IntentRecognitionService intentRecognitionService;

    @BeforeEach
    void clearActions() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
    }

    @Test
    void invalidModelSlotDoesNotCreateActionPreview() {
        when(intentRecognitionService.recognize("删除作业abc"))
                .thenReturn(new RecognizedIntent(
                        AgentIntent.DELETE_ASSIGNMENT,
                        0.95,
                        Map.of("assignmentId", "abc")
                ));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "删除作业abc");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("作业ID格式不正确");
        assertThat(actionRepository.findAll()).isEmpty();
    }
}
