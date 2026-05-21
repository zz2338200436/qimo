package com._202510007517.platform.ai.model;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LocalMockAiModelClient implements AiModelClient {

    @Override
    public Map<String, Object> generateQuestions(GenerateQuestionsRequestDTO request) {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= request.getCount(); i++) {
            questions.add(choiceQuestion(i, request.getTopic() + "相关题目 " + i, request.getDifficulty(), 5));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("topic", request.getTopic());
        result.put("count", request.getCount());
        result.put("difficulty", request.getDifficulty());
        result.put("questions", questions);
        return result;
    }

    @Override
    public Map<String, Object> generateExam(GenerateExamRequestDTO request) {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(choiceQuestion(i, request.getCourseName() + "选择题 " + i, request.getDifficulty(), 10));
        }
        for (int i = 6; i <= 8; i++) {
            questions.add(textQuestion(i, request.getCourseName() + "填空题 " + (i - 5), request.getDifficulty(), "填空题", 10));
        }
        for (int i = 9; i <= 10; i++) {
            questions.add(textQuestion(i, request.getCourseName() + "简答题 " + (i - 8), request.getDifficulty(), "简答题", 25));
        }

        Map<String, Object> exam = new HashMap<>();
        exam.put("title", request.getCourseName() + "模拟试卷");
        exam.put("courseName", request.getCourseName());
        exam.put("totalScore", request.getTotalScore());
        exam.put("duration", request.getDuration());
        exam.put("difficulty", request.getDifficulty());
        exam.put("questions", questions);
        return exam;
    }

    @Override
    public Map<String, Object> generateLearningSuggestions(Long targetStudentId,
                                                           LearningSuggestionRequestDTO request) {
        Map<String, Object> result = new HashMap<>();
        result.put("studentId", targetStudentId);
        result.put("suggestions", List.of(
                "建议加强函数概念的理解",
                "多做积分相关的练习题",
                "参加每周的学习小组讨论"));
        return result;
    }

    private Map<String, Object> choiceQuestion(int id, String content, String difficulty, int score) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("content", content);
        question.put("difficulty", difficulty);
        question.put("type", "选择题");
        question.put("score", score);
        question.put("options", List.of("选项A", "选项B", "选项C", "选项D"));
        question.put("answer", "A");
        return question;
    }

    private Map<String, Object> textQuestion(int id, String content, String difficulty, String type, int score) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("content", content);
        question.put("difficulty", difficulty);
        question.put("type", type);
        question.put("score", score);
        question.put("answer", "正确答案");
        return question;
    }
}
