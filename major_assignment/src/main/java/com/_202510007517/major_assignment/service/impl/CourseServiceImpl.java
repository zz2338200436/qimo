package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.mapper.UserMapper;
import com._202510007517.major_assignment.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.COURSES, key = "'teacher_' + #teacherId", unless = "#result == null")
    public List<Course> findByTeacherId(Long teacherId) {
        List<Course> courses = courseMapper.findByTeacherId(teacherId);
        // 使用批量查询优化N+1问题
        fillStudentCounts(courses);
        return courses;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Long> findCourseIdsByTeacherId(Long teacherId) {
        List<Course> courses = courseMapper.findByTeacherId(teacherId);
        return courses.stream().map(Course::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status) {
        List<Course> courses = courseMapper.findByTeacherIdWithSearch(teacherId, name, courseCode, category, status);
        // 使用批量查询优化N+1问题
        fillStudentCounts(courses);
        return courses;
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.COURSES, key = "'all'", unless = "#result == null")
    public List<Course> getAllCourses() {
        List<Course> courses = courseMapper.getAllCourses();
        // 使用批量查询优化N+1问题
        fillStudentCounts(courses);
        return courses;
    }
    
    /**
     * 批量填充课程的学生数量，解决N+1查询问题
     */
    private void fillStudentCounts(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        Map<Long, Integer> studentCountMap = batchGetStudentCountByCourseIds(courseIds);
        for (Course course : courses) {
            course.setStudentCount(studentCountMap.getOrDefault(course.getId(), 0));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> batchGetStudentCountByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> results = courseMapper.batchGetStudentCountByCourseIds(courseIds);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long courseId = ((Number) row.get("courseId")).longValue();
            Integer count = ((Number) row.get("studentCount")).intValue();
            countMap.put(courseId, count);
        }
        return countMap;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.COURSES, allEntries = true)
    public void create(Course course) {
        // 验证开始日期和结束日期
        validateCourseDates(course);
        courseMapper.insert(course);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.COURSES, allEntries = true)
    public void update(Course course) {
        // 验证开始日期和结束日期
        validateCourseDates(course);
        courseMapper.update(course);
    }
    
    /**
     * 验证课程的开始日期和结束日期
     * @param course 课程对象
     * @throws IllegalArgumentException 如果日期验证失败
     */
    private void validateCourseDates(Course course) {
        String startDate = course.getStartDate();
        String endDate = course.getEndDate();
        
        // 如果两个日期都有值，验证开始日期不能晚于结束日期
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            if (startDate.compareTo(endDate) > 0) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.COURSES, allEntries = true)
    public void delete(Long id) {
        // 删除课程前，先删除所有关联数据（按外键依赖顺序）
        // 1. 删除知识点的关联数据
        courseMapper.deleteAssignmentKnowledgePointsByCourseId(id);
        courseMapper.deleteExamKnowledgePointsByCourseId(id);
        courseMapper.deleteKnowledgeMasteryByCourseId(id);
        courseMapper.deleteQuestionsByCourseId(id);
        // 2. 删除知识点
        courseMapper.deleteKnowledgePointsByCourseId(id);
        // 3. 删除预警记录
        courseMapper.deleteEarlyWarningsByCourseId(id);
        // 4. 删除班级课程关联（class_courses表）
        courseMapper.deleteClassCoursesByCourseId(id);
        // 5. 删除课程班级关联（course_classes表）
        courseMapper.deleteCourseClassesByCourseId(id);
        // 6. 删除作业相关数据（先删除作业班级关联，再删除提交，最后删除作业）
        courseMapper.deleteAssignmentClassesByCourseId(id);
        courseMapper.deleteAssignmentSubmissionsByCourseId(id);
        courseMapper.deleteAssignmentsByCourseId(id);
        // 7. 删除考试相关数据（先删除考试班级关联，再删除提交，最后删除考试）
        courseMapper.deleteExamClassesByCourseId(id);
        courseMapper.deleteExamSubmissionsByCourseId(id);
        courseMapper.deleteExamsByCourseId(id);
        // 8. 最后删除课程
        courseMapper.delete(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.COURSES, key = "#id", unless = "#result == null")
    public Course findById(Long id) {
        Course course = courseMapper.findById(id);
        if (course != null) {
            Integer studentCount = getStudentCountByCourseId(course.getId());
            course.setStudentCount(studentCount);
        }
        return course;
    }
    
    @Override
    public Integer getStudentCountByCourseId(Long courseId) {
        return courseMapper.getStudentCountByCourseId(courseId);
    }
    
    @Override
    public String getTeacherNameByCourseId(Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course != null && course.getTeacherId() != null) {
            var teacher = userMapper.findById(course.getTeacherId());
            return teacher != null ? teacher.getName() : "未知教师";
        }
        return "未知教师";
    }
    
    @Override
    public Map<String, Object> getLearningProgressByCourseIdAndStudentId(Long courseId, Long studentId) {
        Map<String, Object> progress = new HashMap<>();
        
        // 获取作业相关数据
        Integer totalAssignments = assignmentMapper.getAssignmentCountByCourseId(courseId);
        Integer completedAssignments = assignmentMapper.getCompletedAssignmentCountByCourseAndStudent(courseId, studentId);
        
        // 获取考试相关数据
        Integer totalExams = examMapper.getExamCountByCourseId(courseId);
        Integer completedExams = examMapper.getCompletedExamCountByCourseAndStudent(courseId, studentId);
        
        // 确保不出现空指针异常
        totalAssignments = totalAssignments != null ? totalAssignments : 0;
        completedAssignments = completedAssignments != null ? completedAssignments : 0;
        totalExams = totalExams != null ? totalExams : 0;
        completedExams = completedExams != null ? completedExams : 0;
        
        // 计算学习进度百分比
        double assignmentProgress = totalAssignments > 0 ? (double) completedAssignments / totalAssignments : 0;
        double examProgress = totalExams > 0 ? (double) completedExams / totalExams : 0;
        int overallProgress = (int) Math.round((assignmentProgress + examProgress) / 2 * 100);
        
        // 设置返回数据
        progress.put("progress", overallProgress);
        progress.put("completedAssignments", completedAssignments);
        progress.put("totalAssignments", totalAssignments);
        progress.put("completedExams", completedExams);
        progress.put("totalExams", totalExams);
        
        return progress;
    }
    
    @Override
    public List<Map<String, Object>> getClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, String teacherName, Long courseId) {
        return courseMapper.getClassesByTeacherId(teacherId, className, grade, majorName, majorId, teacherName, courseId);
    }
    
    @Override
    public List<Long> getStudentIdsByCourseId(Long courseId) {
        return courseMapper.getStudentIdsByCourseId(courseId);
    }
    
    /*
    @Override
    public List<Long> getStudentIdsByClassId(Long classId) {
        return courseMapper.getStudentIdsByClassId(classId);
    }
    */

    @Override
    public List<Map<String, Object>> getStudentsByCourseId(Long courseId) {
        return courseMapper.getStudentsByCourseId(courseId);
    }
    
    @Override
    public List<Course> findStudentCourses(Long studentId) {
        // 调用StudentMapper的getStudentCourses方法获取学生课程
        List<Course> courses = studentMapper.getStudentCourses(studentId);
        if (courses != null) {
            for (Course course : courses) {
                if (course != null && course.getId() != null) {
                    Integer progress = studentMapper.getCourseProgress(studentId, course.getId());
                    course.setProgress(progress != null ? progress : 0);
                }
            }
        }
        return courses;
    }
    
    // 班级管理相关方法实现
    @Override
    public void createClass(Map<String, Object> classData) {
        courseMapper.createClass(classData);
    }
    
    @Override
    public void updateClass(Map<String, Object> classData) {
        courseMapper.updateClass(classData);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long classId) {
        // 删除所有外键关联记录，避免外键约束错误
        // 1. 删除作业班级关联记录
        courseMapper.deleteAssignmentClassesByClassId(classId);
        // 2. 删除学生班级关联记录
        courseMapper.deleteClassStudentsByClassId(classId);
        // 3. 删除考试班级关联记录
        courseMapper.deleteExamClassesByClassId(classId);
        // 4. 删除班级课程关联记录（class_courses表）
        // 需要先查询该班级的所有课程关联，然后逐个删除
        List<Map<String, Object>> classCourses = courseMapper.getCoursesByClassId(classId);
        for (Map<String, Object> classCourse : classCourses) {
            Long classCourseId = ((Number) classCourse.get("classCourseId")).longValue();
            // 删除class_courses表中的记录
            Map<String, Object> params = new HashMap<>();
            params.put("classId", classId);
            params.put("courseId", classCourse.get("courseId"));
            courseMapper.unassignClassCourse(params);
        }
        // 5. 最后删除班级记录
        courseMapper.deleteClass(classId);
    }
    
    // 课程分配相关方法实现
    @Override
    public List<Map<String, Object>> getClassAssignments(Long teacherId, Long courseId, Long classId) {
        return courseMapper.getClassAssignments(teacherId, courseId, classId);
    }
    
    @Override
    public void assignCourse(Map<String, Object> assignData) {
        courseMapper.assignCourse(assignData);
    }
    
    @Override
    public void unassignCourse(Long assignmentId) {
        // 直接删除 class_courses 表中的课程分配记录
        // assignmentId 是 class_courses 表的主键 ID
        courseMapper.unassignCourse(assignmentId);
    }
}
