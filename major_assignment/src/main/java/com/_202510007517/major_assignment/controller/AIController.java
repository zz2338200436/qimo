package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * AI智能辅助控制器
 * 
 * <p>该控制器提供AI相关的智能辅助功能，包括：
 * <ul>
 *   <li>智能题目生成 - 根据主题、数量和难度自动生成练习题</li>
 *   <li>智能试卷生成 - 根据课程信息自动组卷</li>
 *   <li>个性化学习建议 - 基于学生学习情况提供针对性建议</li>
 * </ul>
 * 
 * <p>所有接口都需要用户登录后才能访问，通过Session进行身份验证。
 * 
 * <p>API基础路径: /api/ai
 * 
 * @author 202510007517
 * @version 1.0
 * @since 2025-01-01
 */
@RestController
@RequestMapping("/api/ai")
public class AIController extends BaseController {

    /** 日志记录器，用于记录AI相关操作的日志信息 */
    private static final Logger logger = LogUtil.getLogger(AIController.class);

    private final CourseMapper courseMapper;

    public AIController(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    /**
     * AI智能生成题目接口
     * 
     * <p>根据指定的主题、数量和难度级别，使用AI技术自动生成练习题目。
     * 目前实现为模拟数据，后续可接入真实的AI模型（如GPT、文心一言等）。
     * 
     * <p>请求示例:
     * <pre>
     * POST /api/ai/generate-questions
     * {
     *   "topic": "Java基础",
     *   "count": 5,
     *   "difficulty": "中等"
     * }
     * </pre>
     * 
     * <p>响应示例:
     * <pre>
     * {
     *   "code": 200,
     *   "message": "生成题目成功",
     *   "data": {
     *     "topic": "Java基础",
     *     "count": 5,
     *     "difficulty": "中等",
     *     "questions": [...]
     *   }
     * }
     * </pre>
     * 
     * @param request 请求参数Map，包含以下字段：
     *                <ul>
     *                  <li>topic - 题目主题/知识点（必填）</li>
     *                  <li>count - 生成题目数量（必填）</li>
     *                  <li>difficulty - 难度级别：简单/中等/困难（必填）</li>
     *                </ul>
     * @param session HTTP会话，用于验证用户登录状态
     * @return ResponseResult 包含生成的题目列表
     *         <ul>
     *           <li>成功(200) - 返回题目数据</li>
     *           <li>未授权(401) - 用户未登录</li>
     *           <li>服务器错误(500) - 生成失败</li>
     *         </ul>
     */
    @PostMapping("/generate-questions")
    public ResponseResult<Map<String, Object>> generateQuestions(@RequestBody Map<String, Object> request,
                                                                 HttpServletRequest requestContext) {
        // 记录请求日志，便于问题追踪和审计
        LogUtil.logRequest(logger, "POST", "/api/ai/generate-questions", request, getCurrentUserId(requestContext));
        
        // 验证用户登录状态，未登录则拒绝访问
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权使用AI生成题目", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 解析请求参数（空值检查）
            Object topicObj = request.get("topic");
            Object countObj = request.get("count");
            Object difficultyObj = request.get("difficulty");

            if (topicObj == null || countObj == null || difficultyObj == null) {
                return ResponseResult.failure("缺少必要参数：topic, count, difficulty", 400);
            }

            String topic = topicObj.toString();           // 题目主题
            Integer count = Integer.valueOf(countObj.toString());  // 生成数量
            String difficulty = difficultyObj.toString(); // 难度级别
            
            // 模拟AI生成题目（实际项目中可替换为真实AI接口调用）
            // TODO: 接入真实AI模型，如OpenAI GPT、百度文心一言等
            List<Map<String, Object>> questions = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i);                                    // 题目ID
                question.put("content", topic + "相关题目 " + i);          // 题目内容
                question.put("difficulty", difficulty);                   // 难度级别
                question.put("type", "选择题");                           // 题目类型
                question.put("options", List.of("选项A", "选项B", "选项C", "选项D")); // 选项列表
                question.put("answer", "A");                              // 正确答案
                questions.add(question);
            }
            
            // 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("topic", topic);
            result.put("count", count);
            result.put("difficulty", difficulty);
            result.put("questions", questions);
            
            // 记录响应日志
            LogUtil.logResponse(logger, "POST", "/api/ai/generate-questions", 200, result, getCurrentUserId(requestContext));
            return ResponseResult.success(result, "生成题目成功", 200);
        } catch (Exception e) {
            // 记录错误日志并返回失败响应
            LogUtil.logError(logger, "生成题目失败", e);
            return ResponseResult.failure("生成题目失败", 500);
        }
    }
    
    // 使用AI生成试卷
    @PostMapping("/generate-exam")
    public ResponseResult<Map<String, Object>> generateExam(@RequestBody Map<String, Object> request,
                                                            HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "POST", "/api/ai/generate-exam", request, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权使用AI生成试卷", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 这里是AI生成试卷的逻辑，目前返回模拟数据
            Object courseNameObj = request.get("courseName");
            Object totalScoreObj = request.get("totalScore");
            Object durationObj = request.get("duration");
            Object difficultyObj = request.get("difficulty");

            if (courseNameObj == null || totalScoreObj == null || durationObj == null || difficultyObj == null) {
                return ResponseResult.failure("缺少必要参数：courseName, totalScore, duration, difficulty", 400);
            }

            String courseName = courseNameObj.toString();
            Integer totalScore = Integer.valueOf(totalScoreObj.toString());
            Integer duration = Integer.valueOf(durationObj.toString());
            String difficulty = difficultyObj.toString();
            
            // 模拟生成试卷
            List<Map<String, Object>> questions = new ArrayList<>();
            // 选择题
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i);
                question.put("content", courseName + "选择题 " + i);
                question.put("difficulty", difficulty);
                question.put("type", "选择题");
                question.put("score", 10);
                question.put("options", List.of("选项A", "选项B", "选项C", "选项D"));
                question.put("answer", "A");
                questions.add(question);
            }
            // 填空题
            for (int i = 6; i <= 8; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i);
                question.put("content", courseName + "填空题 " + (i-5));
                question.put("difficulty", difficulty);
                question.put("type", "填空题");
                question.put("score", 10);
                question.put("answer", "正确答案");
                questions.add(question);
            }
            // 简答题
            for (int i = 9; i <= 10; i++) {
                Map<String, Object> question = new HashMap<>();
                question.put("id", i);
                question.put("content", courseName + "简答题 " + (i-8));
                question.put("difficulty", difficulty);
                question.put("type", "简答题");
                question.put("score", 25);
                question.put("answer", "详细的正确答案");
                questions.add(question);
            }
            
            Map<String, Object> exam = new HashMap<>();
            exam.put("title", courseName + "模拟试卷");
            exam.put("courseName", courseName);
            exam.put("totalScore", totalScore);
            exam.put("duration", duration);
            exam.put("difficulty", difficulty);
            exam.put("questions", questions);
            
            LogUtil.logResponse(logger, "POST", "/api/ai/generate-exam", 200, exam, getCurrentUserId(requestContext));
            return ResponseResult.success(exam, "生成试卷成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "生成试卷失败", e);
            return ResponseResult.failure("生成试卷失败", 500);
        }
    }
    
    // AI学习建议
    @PostMapping("/learning-suggestions")
    public ResponseResult<Map<String, Object>> getLearningSuggestions(@RequestBody Map<String, Object> request,
                                                                      HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "POST", "/api/ai/learning-suggestions", request, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权使用AI学习建议", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            Long currentUserId = getCurrentUserId(requestContext);
            List<String> currentRoles = getCurrentRoles(requestContext);
            Long studentId;

            if (currentRoles.contains(RoleConstants.STUDENT)) {
                studentId = currentUserId;
            } else {
                Object studentIdObj = request.get("studentId");
                if (studentIdObj == null) {
                    return ResponseResult.failure("缺少必要参数：studentId", 400);
                }
                studentId = Long.valueOf(studentIdObj.toString());
            }

            if (currentRoles.contains(RoleConstants.TEACHER)) {
                List<Long> visibleStudentIds = courseMapper.getStudentIdsByClassTeacherId(currentUserId);
                if (visibleStudentIds == null || !visibleStudentIds.contains(studentId)) {
                    return ResponseResult.failure("无权查看该学生的学习建议", 403);
                }
            } else if (!currentRoles.contains(RoleConstants.STUDENT)
                    && !currentRoles.contains(RoleConstants.ADMIN)) {
                return ResponseResult.failure("当前角色无权使用该功能", 403);
            }
            
            // 模拟生成学习建议
            List<String> suggestions = new ArrayList<>();
            suggestions.add("建议加强函数概念的理解");
            suggestions.add("多做积分相关的练习题");
            suggestions.add("参加每周的学习小组讨论");
            
            Map<String, Object> result = new HashMap<>();
            result.put("studentId", studentId);
            result.put("suggestions", suggestions);
            
            LogUtil.logResponse(logger, "POST", "/api/ai/learning-suggestions", 200, result, getCurrentUserId(requestContext));
            return ResponseResult.success(result, "获取学习建议成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "获取学习建议失败", e);
            return ResponseResult.failure("获取学习建议失败", 500);
        }
    }
}
