package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.Exam;

import java.util.List;
import java.util.Map;

public interface ExamService {
    // 获取所有考试
    List<Exam> getAllExams();

    // 根据考试ID获取考试详情
    Exam getExamById(Long id);

    // 根据课程ID获取考试列表
    List<Exam> getExamsByCourseId(Long courseId);

    // 创建考试
    void create(Exam exam);

    // 更新考试
    void update(Exam exam);

    // 删除考试
    void delete(Long id);
    
    // 获取考试列表（支持分页、排序和筛选）
    Map<String, Object> getExamsWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, 
                                             Long courseId, Boolean isActive, Boolean submitted);
    
    // 获取带有提交信息的考试详情
    Map<String, Object> getExamDetailsWithSubmissions(Long examId);
}
