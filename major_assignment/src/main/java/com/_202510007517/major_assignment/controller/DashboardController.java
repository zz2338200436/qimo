package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceProfileClient userServiceProfileClient;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private StudentMapper studentMapper;
    
    // 获取当前学生的学习表现
    @GetMapping("/student-performance")
    public ResponseResult<StudentDashboardDTO> getCurrentStudentPerformance(HttpServletRequest requestContext) {
        logger.debug("获取当前学生学习表现请求");
        
        if (!isLoggedIn(requestContext)) {
            logger.debug("用户未登录");
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long studentId = getCurrentUserId(requestContext);
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
    public ResponseResult<StudentDashboardDTO> getStudentPerformance(@PathVariable Long studentId, HttpServletRequest requestContext) {
        logger.debug("获取学生学习表现请求：studentId={}", studentId);
        
        if (!isLoggedIn(requestContext)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        if (!studentExists(studentId)) {
            return ResponseResult.failure("学生不存在", 404);
        }
        
        StudentDashboardDTO performance = studentService.getStudentPerformance(studentId);
        
        logger.debug("学生学习表现：{}", performance);
        
        return ResponseResult.success(performance, "获取学生学习表现成功", 200);
    }

    private boolean studentExists(Long studentId) {
        if (userServiceProfileClient.getStudentProfile(studentId).isPresent()) {
            return true;
        }
        User student = userService.findById(studentId);
        return student != null;
    }
}

