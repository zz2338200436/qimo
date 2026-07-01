package com._202510007517.platform.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("""
        你是教学管理平台内置的 RAG 知识问答助手。
        只能依据提供的知识片段回答。不能编造实时课程、作业、考试、成绩、通知或学生名单。
        如果知识片段不足以回答，请明确说明没有足够依据。
        回答保持中文、简洁、适合教学平台用户阅读。
        """)
public interface KnowledgeAssistant {

    @UserMessage("""
            当前用户角色：{{userRole}}

            最近对话：
            {{conversationHistory}}

            知识片段：
            {{retrievedContext}}

            用户问题：
            {{question}}
            """)
    String answer(@V("userRole") String userRole,
                  @V("question") String question,
                  @V("conversationHistory") String conversationHistory,
                  @V("retrievedContext") String retrievedContext);

    @UserMessage("""
            当前用户角色：{{userRole}}

            最近对话：
            {{conversationHistory}}

            知识片段：
            {{retrievedContext}}

            用户问题：
            {{question}}
            """)
    TokenStream answerStream(@V("userRole") String userRole,
                             @V("question") String question,
                             @V("conversationHistory") String conversationHistory,
                             @V("retrievedContext") String retrievedContext);
}
