package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统数据控制器，提供动态下拉列表数据
 */
@RestController
@RequestMapping("/api/system")
public class SystemDataController extends BaseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMapper courseMapper;

    /**
     * 获取所有学期选项
     * @param session HTTP会话
     * @return 学期列表
     */
    @GetMapping("/semesters")
    public ResponseResult<List<String>> getSemesters(HttpSession session) {
        // 从数据库获取所有课程，提取不同的学期值
        List<Course> courses = courseMapper.getAllCourses();
        Set<String> semesters = courses.stream()
                .map(Course::getSemester)
                .filter(semester -> semester != null && !semester.isEmpty())
                .collect(Collectors.toSet());
        
        // 转换为列表并返回
        return ResponseResult.success(semesters.stream().sorted().collect(Collectors.toList()), "获取学期列表成功", 200);
    }

    /**
     * 获取所有课程选项（学生视角）
     * @param session HTTP会话
     * @return 课程列表
     */
    @GetMapping("/student/courses")
    public ResponseResult<List<Course>> getStudentCourses(HttpSession session) {
        // 检查登录状态
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long studentId = getCurrentUserId(session);
        // 从数据库获取学生的所有课程
        List<Course> courses = courseService.findStudentCourses(studentId);
        
        return ResponseResult.success(courses, "获取课程列表成功", 200);
    }

    /**
     * 获取所有课程选项（教师视角）
     * @param session HTTP会话
     * @return 课程列表
     */
    @GetMapping("/teacher/courses")
    public ResponseResult<List<Course>> getTeacherCourses(HttpSession session) {
        // 检查登录状态
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
        // 从数据库获取教师的所有课程
        List<Course> courses = courseMapper.findByTeacherId(teacherId);
        
        return ResponseResult.success(courses, "获取课程列表成功", 200);
    }

    /**
     * 获取所有时间范围选项（静态数据）
     * @return 时间范围列表
     */
    @GetMapping("/time-ranges")
    public ResponseResult<List<String>> getTimeRanges() {
        // 时间范围是静态数据，直接返回
        List<String> timeRanges = List.of("week", "month", "quarter", "semester");
        return ResponseResult.success(timeRanges, "获取时间范围列表成功", 200);
    }
}
