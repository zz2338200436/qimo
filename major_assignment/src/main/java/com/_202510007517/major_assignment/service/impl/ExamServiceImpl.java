package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.service.ExamService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamSubmissionService examSubmissionService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserService userService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.EXAMS, key = "'all'", unless = "#result == null")
    public List<Exam> getAllExams() {
        return examMapper.getAllExams();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.EXAMS, key = "#id", unless = "#result == null")
    public Exam getExamById(Long id) {
        return examMapper.getExamById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.EXAMS, key = "'course_' + #courseId", unless = "#result == null")
    public List<Exam> getExamsByCourseId(Long courseId) {
        return examMapper.getExamsByCourseId(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.EXAMS, allEntries = true)
    public void create(Exam exam) {
        examMapper.insert(exam);
        
        // 为新考试自动建立班级关联：保证学生端能够查询到老师发布的考试
        if (exam.getId() != null 
                && exam.getCourseId() != null 
                && exam.getTeacherId() != null) {
            examMapper.insertExamClassesForCourseAndTeacher(
                    exam.getId(),
                    exam.getCourseId(),
                    exam.getTeacherId()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.EXAMS, allEntries = true)
    public void update(Exam exam) {
        examMapper.update(exam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.EXAMS, allEntries = true)
    public void delete(Long id) {
        // 先删除相关的提交记录
        examMapper.deleteExamSubmissionsByExamId(id);
        // 再删除相关的班级关联记录
        examMapper.deleteExamClassesByExamId(id);
        // 最后删除考试本身
        examMapper.delete(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getExamsWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, 
                                                   Long courseId, Boolean isActive, Boolean submitted) {
        // 安全处理分页参数
        int safePage = PageUtils.safePage(page, 1);
        int safeSize = PageUtils.safeSize(size, 10, 100);
        
        // 直接基于学生ID查询其关联到的所有考试（通过exam_classes表关联）
        List<Exam> studentExams = examMapper.getExamsByStudentId(studentId);
        
        // 应用筛选条件
        List<Exam> filteredExams = studentExams.stream()
                .filter(exam -> {
                    // 课程ID筛选
                    if (courseId != null) {
                        if (!exam.getCourseId().equals(courseId)) {
                            return false;
                        }
                    }
                    // 考试状态筛选
                    if (isActive != null) {
                        if (exam.getIsActive() != isActive) {
                            return false;
                        }
                    }
                    // 提交状态筛选
                    if (submitted != null) {
                        // 这里需要检查学生是否提交了该考试
                        // 由于当前没有相关的Mapper方法，暂时不实现
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // 计算总数
        int totalElements = filteredExams.size();
        
        // 应用分页
        List<Exam> pagedExams = PageUtils.paginate(filteredExams, safePage, safeSize);
        
        // 为每个考试补充课程、教师和提交信息
        List<Map<String, Object>> examsWithDetails = new ArrayList<>();
        for (Exam exam : pagedExams) {
            Map<String, Object> examMap = new HashMap<>();
            examMap.put("id", exam.getId());
            examMap.put("title", exam.getTitle());
            examMap.put("description", exam.getDescription());
            examMap.put("startTime", exam.getStartTime());
            examMap.put("endTime", exam.getEndTime());
            examMap.put("duration", exam.getDuration());
            examMap.put("isActive", exam.getIsActive());
            examMap.put("isOnline", exam.getIsOnline());
            examMap.put("location", exam.getLocation());
            examMap.put("publishDate", exam.getPublishDate());
            examMap.put("courseId", exam.getCourseId());
            examMap.put("teacherId", exam.getTeacherId());
            
            // 课程、教师名称
            Course course = courseService.findById(exam.getCourseId());
            if (course != null) {
                examMap.put("courseName", course.getCourseName());
            }
            User teacher = userService.findById(exam.getTeacherId());
            if (teacher != null) {
                examMap.put("teacherName", teacher.getName());
            }
            
            // 学生提交
            examMap.put("submission", examSubmissionService.getSubmissionByExamAndStudent(exam.getId(), studentId));
            examsWithDetails.add(examMap);
        }
        
        // 使用工具类构建分页响应
        return PageUtils.buildPageResponse(examsWithDetails, safePage, safeSize, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getExamDetailsWithSubmissions(Long examId) {
        // 获取考试基本信息
        Exam exam = examMapper.getExamById(examId);
        if (exam == null) {
            return null;
        }
        
        // 构建考试详情响应
        Map<String, Object> examDetails = new HashMap<>();
        examDetails.put("id", exam.getId());
        examDetails.put("title", exam.getTitle());
        examDetails.put("description", exam.getDescription());
        examDetails.put("startTime", exam.getStartTime());
        examDetails.put("endTime", exam.getEndTime());
        examDetails.put("duration", exam.getDuration());
        examDetails.put("isActive", exam.getIsActive());
        examDetails.put("isOnline", exam.getIsOnline());
        examDetails.put("location", exam.getLocation());
        examDetails.put("publishDate", exam.getPublishDate());
        examDetails.put("courseId", exam.getCourseId());
        examDetails.put("teacherId", exam.getTeacherId());
        
        // 由于缺少getCourseName和getTeacherName方法，暂时不获取这些信息
        // 可以后续通过其他服务或直接从数据库获取
        
        return examDetails;
    }
}
