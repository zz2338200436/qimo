package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.mapper.AssignmentSubmissionMapper;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentSubmissionServiceImpl implements AssignmentSubmissionService {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentSubmissionServiceImpl.class);
    
    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Autowired
    private AssignmentMapper assignmentMapper;
    @Autowired
    private KnowledgeMasteryService knowledgeMasteryService;
    @Autowired
    private EarlyWarningAnalysisService earlyWarningAnalysisService;

    @Override
    public AssignmentSubmission submitAssignment(Long assignmentId, Long studentId, String content) {
        // 1. 检查作业是否存在
        Assignment assignment = assignmentMapper.getAssignmentById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("作业不存在");
        }
        
        // 2. 检查学生是否有权限提交该作业（暂时简化处理，实际应该检查学生是否选修了该课程）
        
        // 3. 检查是否已经提交过该作业
        AssignmentSubmission existingSubmission = assignmentSubmissionMapper.findByAssignmentAndStudent(assignmentId, studentId);
        
        Date now = new Date();
        boolean isLate = false;
        int latePenalty = 0;
        
        // 4. 检查是否迟到（只有第一次提交需要检查）
        if (existingSubmission == null) {
            isLate = now.after(assignment.getDueDate());
            if (isLate) {
                // 计算迟到惩罚（这里简化处理，实际可以根据迟到时间长短计算）
                latePenalty = 10;
            }
        }
        
        if (existingSubmission != null) {
            // 如果已经提交过，更新提交内容
            existingSubmission.setContent(content);
            existingSubmission.setSubmissionDate(now);
            existingSubmission.setGraded(false); // 重新提交后需要重新批改
            
            if (assignmentSubmissionMapper.fullUpdateSubmission(existingSubmission) > 0) {
                return existingSubmission;
            }
            return null;
        } else {
            // 第一次提交，创建新的提交记录
            AssignmentSubmission submission = new AssignmentSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setGraded(false);
            submission.setIsLate(isLate);
            submission.setLatePenalty(latePenalty);
            submission.setScore(0);
            submission.setSubmissionDate(now);
            submission.setTeacherComment("");
            submission.setContent(content);
            
            if (assignmentSubmissionMapper.insertSubmission(submission) > 0) {
                return submission;
            }
            return null;
        }
    }

    @Override
    public AssignmentSubmission getSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId) {
        return assignmentSubmissionMapper.findByAssignmentAndStudent(assignmentId, studentId);
    }

    @Override
    public boolean gradeAssignment(Long submissionId, Integer score, String teacherComment) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(submissionId);
        submission.setScore(score);
        submission.setTeacherComment(teacherComment);
        submission.setGraded(true);
        
        boolean success = assignmentSubmissionMapper.updateSubmission(submission) > 0;
        
        // 如果批改成功，自动更新知识点掌握情况和学情预警
        if (success) {
            try {
                // 获取提交记录以获取学生ID和作业ID
                AssignmentSubmission gradedSubmission = assignmentSubmissionMapper.findById(submissionId);
                if (gradedSubmission != null) {
                    Long studentId = gradedSubmission.getStudentId();
                    Long assignmentId = gradedSubmission.getAssignmentId();
                    
                    // 获取作业信息以获取总分和课程ID
                    Assignment assignment = assignmentMapper.getAssignmentById(assignmentId);
                    Integer totalScore = assignment != null ? assignment.getMaxScore() : 100;
                    
                    // 更新知识点掌握情况
                    knowledgeMasteryService.updateMasteryByAssignment(submissionId, studentId, assignmentId, totalScore);
                    
                    // 异步触发学情预警分析
                    if (assignment != null && assignment.getCourseId() != null) {
                        final Long courseId = assignment.getCourseId();
                        new Thread(() -> {
                            try {
                                earlyWarningAnalysisService.analyzeStudentWarningsRealtime(studentId, courseId);
                                logger.info("作业批改后学情预警分析完成，学生ID: {}, 课程ID: {}", studentId, courseId);
                            } catch (Exception e) {
                                logger.warn("作业批改后学情预警分析失败，但不影响批改结果", e);
                            }
                        }).start();
                    }
                }
            } catch (Exception e) {
                // 静默处理异常，不影响批改结果
                logger.warn("批改后处理失败，但不影响批改结果", e);
            }
        }
        
        return success;
    }

    @Override
    public AssignmentSubmission getSubmissionById(Long submissionId) {
        return assignmentSubmissionMapper.findById(submissionId);
    }

    @Override
    public List<AssignmentSubmission> getAllSubmissions() {
        return assignmentSubmissionMapper.findAll();
    }

    @Override
    public List<AssignmentSubmission> getSubmissionsByAssignmentId(Long assignmentId) {
        return assignmentSubmissionMapper.findByAssignmentId(assignmentId);
    }

    @Override
    public List<AssignmentSubmission> getSubmissionsByStudentId(Long studentId) {
        return assignmentSubmissionMapper.findByStudentId(studentId);
    }

    @Override
    public boolean deleteSubmission(Long submissionId) {
        return assignmentSubmissionMapper.deleteSubmission(submissionId) > 0;
    }

    @Override
    public boolean updateSubmission(AssignmentSubmission submission) {
        return assignmentSubmissionMapper.fullUpdateSubmission(submission) > 0;
    }

    @Override
    public List<AssignmentSubmission> getSubmissionsWithPagination(Integer page, Integer size, Integer total, String sortBy, String order, Long assignmentId, Long studentId, Boolean graded) {
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(
                page == null ? 1 : page,
                size == null ? PageUtils.DEFAULT_PAGE_SIZE : size,
                total == null ? 0 : total);
        return assignmentSubmissionMapper.findWithPagination(
                assignmentId,
                studentId,
                graded,
                sortBy,
                order,
                window.offset(),
                window.size());
    }

    @Override
    public Integer countSubmissions(Long assignmentId, Long studentId, Boolean graded) {
        return assignmentSubmissionMapper.countSubmissions(assignmentId, studentId, graded);
    }
}
