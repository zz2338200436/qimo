package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;

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
 * <p>所有接口都需要用户登录后才能访问，通过请求上下文中的统一登录态进行身份验证。
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
     * 当前单体接口已下线模拟实现，统一引导调用独立 AI 服务。
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
     * @param requestContext HTTP请求上下文，用于验证用户登录状态
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

        return aiCapabilityMovedResponse("POST", "/api/ai/generate-questions", getCurrentUserId(requestContext));
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

        return aiCapabilityMovedResponse("POST", "/api/ai/generate-exam", getCurrentUserId(requestContext));
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

        return aiCapabilityMovedResponse("POST", "/api/ai/learning-suggestions", currentUserId);
    }

    private ResponseResult<Map<String, Object>> aiCapabilityMovedResponse(String method, String url, Long userId) {
        String message = "AI能力已迁移至独立服务，请改用 ai-service 或网关聚合后的新接口";
        LogUtil.logWarning(logger, message, userId);
        LogUtil.logResponse(logger, method, url, 501, message, userId);
        return ResponseResult.failure(message, 501);
    }
}
