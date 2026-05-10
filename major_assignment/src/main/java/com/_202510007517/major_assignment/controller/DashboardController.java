package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private StudentMapper studentMapper;
    
    // 获取当前学生的学习表现
    @GetMapping("/student-performance")
    public ResponseResult<StudentDashboardDTO> getCurrentStudentPerformance(HttpSession session) {
        logger.debug("获取当前学生学习表现请求");
        
        if (!isLoggedIn(session)) {
            logger.debug("用户未登录");
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long studentId = getCurrentUserId(session);
        logger.debug("当前学生ID：{}", studentId);
        
        try {
            StudentDashboardDTO performance = studentService.getStudentPerformance(studentId);
            logger.debug("当前学生学习表现：{}", performance);
            return ResponseResult.success(performance, "获取学生学习表现成功", 200);
        } catch (Exception e) {
            logger.error("获取学生学习表现失败：{}", e.getMessage(), e);
            return ResponseResult.failure("获取学生学习表现失败：" + e.getMessage(), 500);
        }
    }
    
    // 获取指定学生的学习表现
    @GetMapping("/student-performance/{studentId}")
    public ResponseResult<StudentDashboardDTO> getStudentPerformance(@PathVariable Long studentId, HttpSession session) {
        logger.debug("获取学生学习表现请求：studentId={}", studentId);
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 检查学生是否存在
        User student = userService.findById(studentId);
        if (student == null) {
            return ResponseResult.failure("学生不存在", 404);
        }
        
        StudentDashboardDTO performance = studentService.getStudentPerformance(studentId);
        
        logger.debug("学生学习表现：{}", performance);
        
        return ResponseResult.success(performance, "获取学生学习表现成功", 200);
    }
}
