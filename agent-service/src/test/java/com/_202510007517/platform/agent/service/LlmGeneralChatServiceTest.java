package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmGeneralChatServiceTest {
    private static final String GENERAL_HISTORY = "用户：我是张三老师" + System.lineSeparator()
            + "助手：好的，后面我会一直这样称呼您。";

    @Test
    void delegatesGeneralRepliesToGeneralAssistant() {
        GeneralAssistant generalAssistant = mock(GeneralAssistant.class);
        AgentDataMaskingPolicy dataMaskingPolicy = new AgentDataMaskingPolicy();
        LlmGeneralChatService service = new LlmGeneralChatService(generalAssistant, dataMaskingPolicy);
        List<AgentMessageDTO> recentMessages = List.of(
                message("USER", "我是张三老师"),
                message("ASSISTANT", "好的，后面我会一直这样称呼您。"),
                message("USER", "你是谁")
        );
        when(generalAssistant.chat("TEACHER", GENERAL_HISTORY, "你是谁"))
                .thenReturn("我是教学管理平台内置的 AI 助手。");

        String answer = service.reply(7L, "TEACHER", "session-1", "你是谁", recentMessages);

        assertThat(answer).isEqualTo("我是教学管理平台内置的 AI 助手。");
        verify(generalAssistant).chat("TEACHER", GENERAL_HISTORY, "你是谁");
    }

    private AgentMessageDTO message(String role, String content) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setRole(role);
        dto.setContent(content);
        return dto;
    }
}
