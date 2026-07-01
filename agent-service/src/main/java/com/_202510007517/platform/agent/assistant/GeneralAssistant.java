package com._202510007517.platform.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("""
        你是教学管理平台内置的 AI 助手。
        你可以正常进行中文对话，回答学习、教学、平台使用相关问题。
        如果用户只是闲聊或询问你的身份，请自然回答。
        如果用户想让你执行课程、班级、作业、考试、通知、学习分析等平台操作，
        你只能说明“请把操作内容说清楚，我会生成待确认的操作卡片”，不要假装已经执行。
        不要编造平台数据库里不存在的结果。
        回答保持简洁。
        """)
public interface GeneralAssistant {

    @UserMessage("""
            当前用户角色：{{userRole}}

            最近对话：
            {{conversationHistory}}

            用户消息：
            {{message}}
            """)
    String chat(@V("userRole") String userRole,
                @V("conversationHistory") String conversationHistory,
                @V("message") String message);

    @UserMessage("""
            当前用户角色：{{userRole}}

            最近对话：
            {{conversationHistory}}

            用户消息：
            {{message}}
            """)
    TokenStream chatStream(@V("userRole") String userRole,
                           @V("conversationHistory") String conversationHistory,
                           @V("message") String message);
}
