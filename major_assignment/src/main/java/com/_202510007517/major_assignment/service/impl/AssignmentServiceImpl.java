package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.service.AssignmentService;
import com._202510007517.major_assignment.service.StudentService;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentServiceImpl implements AssignmentService {
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserService userService;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.ASSIGNMENTS, key = "'all'", unless = "#result == null")
    public List<Assignment> getAllAssignments() {
        return assignmentMapper.getAllAssignments();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.ASSIGNMENTS, key = "#id", unless = "#result == null")
    public Assignment getAssignmentById(Long id) {
        return assignmentMapper.getAssignmentById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.ASSIGNMENTS, key = "'course_' + #courseId", unless = "#result == null")
    public List<Assignment> getAssignmentsByCourseId(Long courseId) {
        return assignmentMapper.getAssignmentsByCourseId(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public void create(Assignment assignment) {
        assignmentMapper.insert(assignment);
        
        // 为新作业自动建立班级关联：保证学生端能够查询到老师发布的作业
        if (assignment.getId() != null 
                && assignment.getCourseId() != null 
                && assignment.getTeacherId() != null) {
            assignmentMapper.insertAssignmentClassesForCourseAndTeacher(
                    assignment.getId(),
                    assignment.getCourseId(),
                    assignment.getTeacherId()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public void update(Assignment assignment) {
        assignmentMapper.update(assignment);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public void delete(Long id) {
        // 先删除所有相关的作业提交记录
        assignmentMapper.deleteSubmissionsByAssignmentId(id);
        // 删除作业班级关联记录
        assignmentMapper.deleteAssignmentClassesByAssignmentId(id);
        // 删除作业知识点关联记录
        assignmentMapper.deleteAssignmentKnowledgePointsByAssignmentId(id);
        // 最后删除作业本身
        assignmentMapper.delete(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAssignmentsWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, 
                                                         Long courseId, Boolean submitted, Boolean isActive) {
        // 安全处理分页参数
        int safePage = PageUtils.safePage(page, 1);
        int safeSize = PageUtils.safeSize(size, 10, 100);
        
        // 直接基于学生ID查询其关联到的所有作业（通过班级和课程关联）
        List<Assignment> studentAssignments = assignmentMapper.getAssignmentsByStudentId(studentId);
        
        // 应用筛选条件（在"学生所有作业"的基础上进行过滤）
        List<Assignment> filteredAssignments = studentAssignments.stream()
                .filter(assignment -> {
                    // 课程ID筛选
                    if (courseId != null) {
                        if (!assignment.getCourseId().equals(courseId)) {
                            return false;
                        }
                    }
                    // 提交状态筛选
                    if (submitted != null) {
                        // 这里需要检查学生是否提交了该作业
                        // 由于当前没有相关的Mapper方法，暂时不实现
                    }
                    // 作业状态筛选
                    if (isActive != null) {
                        if (assignment.getIsActive() != isActive) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // 应用排序
        if (sortBy != null && !sortBy.isEmpty()) {
            Comparator<Assignment> comparator = null;
            switch (sortBy.toLowerCase()) {
                case "duedate":
                    comparator = Comparator.comparing(Assignment::getDueDate, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "publishdate":
                    comparator = Comparator.comparing(Assignment::getPublishDate, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "title":
                    comparator = Comparator.comparing(Assignment::getTitle, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "id":
                default:
                    comparator = Comparator.comparing(Assignment::getId, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
            }
            
            if (comparator != null) {
                if ("ASC".equalsIgnoreCase(order)) {
                    filteredAssignments.sort(comparator);
                } else {
                    filteredAssignments.sort(comparator.reversed());
                }
            }
        }
        
        // 计算总数
        int totalElements = filteredAssignments.size();
        
        // 应用分页
        List<Assignment> pagedAssignments = PageUtils.paginate(filteredAssignments, safePage, safeSize);
        
        // 为每个作业添加学生的提交信息
        List<Map<String, Object>> assignmentsWithSubmissions = new ArrayList<>();
        for (Assignment assignment : pagedAssignments) {
            Map<String, Object> assignmentWithSubmission = new HashMap<>();
            assignmentWithSubmission.put("id", assignment.getId());
            assignmentWithSubmission.put("title", assignment.getTitle());
            assignmentWithSubmission.put("description", assignment.getDescription());
            assignmentWithSubmission.put("courseId", assignment.getCourseId());
            assignmentWithSubmission.put("dueDate", assignment.getDueDate());
            assignmentWithSubmission.put("publishDate", assignment.getPublishDate());
            assignmentWithSubmission.put("teacherId", assignment.getTeacherId());
            assignmentWithSubmission.put("isActive", assignment.getIsActive());
            // 课程与教师名称用于前端展示
            Course course = courseService.findById(assignment.getCourseId());
            if (course != null) {
                assignmentWithSubmission.put("courseName", course.getCourseName());
            }
            User teacher = userService.findById(assignment.getTeacherId());
            if (teacher != null) {
                assignmentWithSubmission.put("teacherName", teacher.getName());
            }
            
            // 获取学生的提交信息
            Object submission = assignmentSubmissionService.getSubmissionByAssignmentAndStudent(assignment.getId(), studentId);
            assignmentWithSubmission.put("submission", submission);
            
            assignmentsWithSubmissions.add(assignmentWithSubmission);
        }
        
        // 使用工具类构建分页响应
        return PageUtils.buildPageResponse(assignmentsWithSubmissions, safePage, safeSize, totalElements);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAssignmentDetailsWithSubmissions(Long assignmentId) {
        // 获取作业基本信息
        Assignment assignment = assignmentMapper.getAssignmentById(assignmentId);
        if (assignment == null) {
            return null;
        }
        
        // 构建作业详情响应
        Map<String, Object> assignmentDetails = new HashMap<>();
        assignmentDetails.put("id", assignment.getId());
        assignmentDetails.put("title", assignment.getTitle());
        assignmentDetails.put("description", assignment.getDescription());
        assignmentDetails.put("dueDate", assignment.getDueDate());
        assignmentDetails.put("publishDate", assignment.getPublishDate());
        assignmentDetails.put("isActive", assignment.getIsActive());
        assignmentDetails.put("courseId", assignment.getCourseId());
        assignmentDetails.put("teacherId", assignment.getTeacherId());
        
        // 添加提交记录
        List<Map<String, Object>> submissions = assignmentMapper.getSubmissionsByAssignmentId(assignmentId);
        assignmentDetails.put("submissions", submissions);
        
        return assignmentDetails;
    }
}
