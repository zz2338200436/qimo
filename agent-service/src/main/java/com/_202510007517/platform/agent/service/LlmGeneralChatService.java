package com._202510007517.platform.agent.service;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LlmGeneralChatService implements GeneralChatService {
    private static final Logger log = LoggerFactory.getLogger(LlmGeneralChatService.class);
    private static final String FALLBACK_MESSAGE = "我现在暂时无法连接大模型，请稍后再试。";

    private final ChatModel chatModel;
    private final AgentDataMaskingPolicy dataMaskingPolicy;

    public LlmGeneralChatService(ChatModel chatModel, AgentDataMaskingPolicy dataMaskingPolicy) {
        this.chatModel = chatModel;
        this.dataMaskingPolicy = dataMaskingPolicy;
    }

    @Override
    public String reply(Long userId, String userRole, String message) {
        String prompt = buildPrompt(userRole, message);
        log.info("agent general chat prompt: {}", dataMaskingPolicy.maskText(prompt));
        try {
            String output = chatModel.chat(prompt);
            log.info("agent general chat output: {}", dataMaskingPolicy.maskText(output));
            if (output == null || output.isBlank()) {
                return FALLBACK_MESSAGE;
            }
            return output.trim();
        } catch (RuntimeException ex) {
            log.info("agent general chat failed: reason={}", ex.getClass().getSimpleName());
            return FALLBACK_MESSAGE;
        }
    }

    private String buildPrompt(String userRole, String message) {
        return """
                你是教学管理平台内置的 AI 助手。
                当前用户角色：%s。

                你可以正常进行中文对话，回答学习、教学、平台使用相关问题。
                如果用户只是闲聊或询问你的身份，请自然回答。
                如果用户想让你执行课程、班级、作业、考试、通知、学习分析等平台操作，
                你只能说明“请把操作内容说清楚，我会生成待确认的操作卡片”，不要假装已经执行。
                不要编造平台数据库里不存在的结果。
                回答保持简洁。

                用户消息：
                %s
                """.formatted(userRole == null ? "UNKNOWN" : userRole, message == null ? "" : message);
    }
}
