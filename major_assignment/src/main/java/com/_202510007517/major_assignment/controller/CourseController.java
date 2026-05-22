package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.ErrorMessages;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.constants.SuccessMessages;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.exception.ResourceNotFoundException;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.utils.LogUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/courses")
@Validated
public class CourseController extends BaseController {
    
    // Logger实例
    private static final Logger logger = LogUtil.getLogger(CourseController.class);
    
    @Autowired
    private CourseService courseService;
    
    @GetMapping
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<PageResult<Course>> getCourses(HttpServletRequest requestContext, 
                                                  @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page, 
                                                  @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
                                                  @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
                                                  @RequestParam(value = "order", defaultValue = "DESC") String order,
                                                  @RequestParam(value = "courseName", required = false) String courseName,
                                                  @RequestParam(value = "courseStatus", required = false) String courseStatus,
                                                  @RequestParam(value = "courseCode", required = false) String courseCode,
                                                  @RequestParam(value = "category", required = false) String category) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "GET", "/api/teacher/courses", null, currentUserId);
        
        Long teacherId = currentUserId;
        // 调用带分页和搜索参数的服务方法
        List<Course> courses = courseService.findByTeacherIdWithSearch(teacherId, courseName, courseCode, category, courseStatus);
        
        // 构建分页结果
        PageResult<Course> pageResult = new PageResult<>();
        pageResult.setContent(courses);
        pageResult.setPageNumber(page);
        pageResult.setPageSize(size);
        pageResult.setTotalElements((long) courses.size());
        pageResult.setTotalPages((int) Math.ceil((double) courses.size() / size));
        pageResult.setFirst(page == 1);
        pageResult.setLast(page >= pageResult.getTotalPages());
        pageResult.setOffset((long) (page - 1) * size);
        pageResult.setNumberOfElements(courses.size());
        pageResult.setEmpty(courses.isEmpty());
        
        // 记录响应日志
        LogUtil.logResponse(logger, "GET", "/api/teacher/courses", 200, pageResult, teacherId);
        return ResponseResult.success(pageResult, SuccessMessages.GET_COURSE_LIST_SUCCESS, 200);
    }
    
    @PostMapping
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<Course> createCourse(@RequestBody @Valid Course course, HttpServletRequest requestContext) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "POST", "/api/teacher/courses", course, currentUserId);
        
        Long teacherId = currentUserId;
        course.setTeacherId(teacherId);
        
        try {
            courseService.create(course);
            // 记录业务操作日志
            LogUtil.logOperation(logger, "创建课程", "课程名称: " + course.getCourseName(), teacherId, true);
            // 记录响应日志
            LogUtil.logResponse(logger, "POST", "/api/teacher/courses", 201, course, teacherId);
            return ResponseResult.created(course);
        } catch (IllegalArgumentException e) {
            // 处理日期验证等业务逻辑错误
            LogUtil.logWarning(logger, "创建课程失败: " + e.getMessage(), teacherId);
            return ResponseResult.failure(e.getMessage(), 400);
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logError(logger, "创建课程失败", e);
            return ResponseResult.failure(ErrorMessages.CREATE_FAILED, 500);
        }
    }
    
    @PutMapping("/{id}")
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<Course> updateCourse(@PathVariable Long id, @RequestBody @Valid Course course, HttpServletRequest requestContext) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "PUT", "/api/teacher/courses/" + id, course, currentUserId);
        
        Long teacherId = currentUserId;
        course.setId(id);
        course.setTeacherId(teacherId);
        
        try {
            courseService.update(course);
            // 记录业务操作日志
            LogUtil.logOperation(logger, "更新课程", "课程ID: " + id + ", 课程名称: " + course.getCourseName(), teacherId, true);
            // 记录响应日志
            LogUtil.logResponse(logger, "PUT", "/api/teacher/courses/" + id, 200, course, teacherId);
            return ResponseResult.success(course);
        } catch (IllegalArgumentException e) {
            // 处理日期验证等业务逻辑错误
            LogUtil.logWarning(logger, "更新课程失败: " + e.getMessage(), teacherId);
            return ResponseResult.failure(e.getMessage(), 400);
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logError(logger, "更新课程失败，课程ID: " + id, e);
            return ResponseResult.failure(ErrorMessages.UPDATE_FAILED, 500);
        }
    }
    
    @DeleteMapping("/{id}")
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<Void> deleteCourse(@PathVariable Long id, HttpServletRequest requestContext) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/courses/" + id, null, currentUserId);
        
        Long teacherId = currentUserId;
        
        try {
            courseService.delete(id);
            // 记录业务操作日志
            LogUtil.logOperation(logger, "删除课程", "课程ID: " + id, teacherId, true);
            // 记录响应日志
            LogUtil.logResponse(logger, "DELETE", "/api/teacher/courses/" + id, 204, null, teacherId);
            return ResponseResult.noContent();
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logError(logger, "删除课程失败，课程ID: " + id, e);
            return ResponseResult.failure(ErrorMessages.DELETE_FAILED, 500);
        }
    }
    
    @GetMapping("/{courseId}/students")
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<List<Map<String, Object>>> getCourseStudents(@PathVariable Long courseId, HttpServletRequest requestContext) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "GET", "/api/teacher/courses/" + courseId + "/students", null, currentUserId);
        
        List<Map<String, Object>> students = courseService.getStudentsByCourseId(courseId);
        
        // 记录响应日志
        LogUtil.logResponse(logger, "GET", "/api/teacher/courses/" + courseId + "/students", 200, students, currentUserId);
        return ResponseResult.success(students, SuccessMessages.GET_STUDENTS_SUCCESS, 200);
    }
    
    @GetMapping("/{courseId}")
    @RequireLogin(roles = {RoleConstants.TEACHER})
    public ResponseResult<Course> getCourseDetails(@PathVariable Long courseId, HttpServletRequest requestContext) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 记录请求日志
        LogUtil.logRequest(logger, "GET", "/api/teacher/courses/" + courseId, null, currentUserId);
        
        Course course = courseService.findById(courseId);
        if (course == null) {
            LogUtil.logWarning(logger, "课程不存在，课程ID: " + courseId, currentUserId);
            throw new ResourceNotFoundException(ErrorMessages.COURSE_NOT_FOUND);
        }
        
        // 记录响应日志
        LogUtil.logResponse(logger, "GET", "/api/teacher/courses/" + courseId, 200, course, currentUserId);
        return ResponseResult.success(course);
    }
}
