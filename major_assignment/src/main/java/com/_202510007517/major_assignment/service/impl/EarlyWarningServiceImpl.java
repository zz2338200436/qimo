package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.WarningStatsDTO;
import com._202510007517.major_assignment.entity.dto.WarningStatusUpdateDTO;
import com._202510007517.major_assignment.mapper.EarlyWarningMapper;
import com._202510007517.major_assignment.service.EarlyWarningService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EarlyWarningServiceImpl implements EarlyWarningService {
    
    @Autowired
    private EarlyWarningMapper earlyWarningMapper;
    
    @Override
    public List<EarlyWarning> getUnresolvedWarnings(Long teacherId) {
        List<EarlyWarning> warnings = earlyWarningMapper.findUnresolvedByTeacherId(teacherId);
        
        // 处理状态字段和其他前端需要的字段
        processWarnings(warnings);
        
        return warnings;
    }
    
    @Override
    public WarningStatsDTO getWarningStats(Long teacherId, Long classId, Long courseId) {
        WarningStatsDTO stats = new WarningStatsDTO();
        
        // 使用带筛选条件的统计方法
        stats.setTotalWarnings(earlyWarningMapper.countTotalWarningsWithFilter(teacherId, classId, courseId));
        stats.setPendingWarnings(earlyWarningMapper.countPendingWarningsWithFilter(teacherId, classId, courseId));
        stats.setProcessingWarnings(0L); // 处理中状态暂时与未处理状态合并
        stats.setResolvedWarnings(stats.getTotalWarnings() - stats.getPendingWarnings());
        
        stats.setScoreWarningCount(earlyWarningMapper.countWarningsByTypeWithFilter(teacherId, "low_score", classId, courseId));
        stats.setAttendanceWarningCount(earlyWarningMapper.countWarningsByTypeWithFilter(teacherId, "low_attendance", classId, courseId));
        stats.setHomeworkWarningCount(earlyWarningMapper.countWarningsByTypeWithFilter(teacherId, "late_submission", classId, courseId));
        stats.setProgressWarningCount(earlyWarningMapper.countWarningsByTypeWithFilter(teacherId, "progress", classId, courseId));
        
        return stats;
    }
    
    @Override
    public PageResult<EarlyWarning> getWarningList(Long teacherId, Long classId, Long courseId, 
                                          String warningType, String status, 
                                          Integer page, Integer size) {
        Long totalElements = earlyWarningMapper.countWarningsByCondition(teacherId, classId, courseId,
                warningType, status);
        int total = totalElements != null ? totalElements.intValue() : 0;
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(page, size, total);
        
        // 获取预警列表
        List<EarlyWarning> warnings = earlyWarningMapper.findWarningsByCondition(teacherId, classId, courseId, 
                                                                               warningType, status, window.offset(), window.size());
        
        // 处理预警数据
        processWarnings(warnings);
        return PageUtils.buildPageResult(warnings, window.page(), window.size(), totalElements != null ? totalElements : 0L);
    }
    
    @Override
    public EarlyWarning getWarningDetail(Long warningId) {
        EarlyWarning warning = earlyWarningMapper.findWarningById(warningId);
        if (warning != null) {
            processWarning(warning);
        }
        return warning;
    }
    
    @Override
    public boolean updateWarningStatus(Long warningId, WarningStatusUpdateDTO updateDTO, Long userId) {
        // 创建EarlyWarning对象用于更新
        EarlyWarning warning = new EarlyWarning();
        warning.setId(warningId);
        warning.setStatus(updateDTO.getStatus());
        warning.setResolvedNote(updateDTO.getResolvedNote());
        
        // 更新预警状态
        return earlyWarningMapper.updateWarningStatus(warningId, warning, userId);
    }
    
    @Override
    public List<EarlyWarning> getWarningsForExport(Long teacherId, Long classId, Long courseId, 
                                          String warningType, String status) {
        List<EarlyWarning> warnings = earlyWarningMapper.findWarningsForExport(teacherId, classId, courseId, 
                                                                             warningType, status);
        
        // 处理预警数据
        processWarnings(warnings);
        
        return warnings;
    }
    
    @Override
    public boolean addEarlyWarning(EarlyWarning earlyWarning) {
        return earlyWarningMapper.insert(earlyWarning) > 0;
    }
    
    @Override
    public boolean deleteEarlyWarning(Long id) {
        return earlyWarningMapper.deleteById(id) > 0;
    }

    @Override
    public List<EarlyWarning> getByCourseId(Long courseId, String warningType, String warningLevel, Boolean isResolved) {
        List<EarlyWarning> warnings = earlyWarningMapper.findByCourseId(courseId, warningType, warningLevel, isResolved);
        processWarnings(warnings);
        return warnings;
    }
    
    // 批量处理预警数据
    private void processWarnings(List<EarlyWarning> warnings) {
        for (EarlyWarning warning : warnings) {
            processWarning(warning);
        }
    }
    
    // 单个处理预警数据
    private void processWarning(EarlyWarning warning) {
        warning.setStatus(warning.getIsResolved() ? "resolved" : "pending");
        warning.setReason(warning.getWarningMessage());
        warning.setSuggestion(getSuggestionByWarningType(warning.getWarningType()));
    }
    
    private String getSuggestionByWarningType(String warningType) {
        switch (warningType) {
            case "absent":
            case "low_attendance":
                return "建议与学生家长联系，了解缺勤原因";
            case "low_score":
                return "建议安排课后辅导，重点讲解薄弱知识点";
            case "late_submission":
            case "homework":
                return "建议与学生沟通，了解作业完成困难";
            case "progress":
                return "建议关注学生学习进度，提供额外辅导";
            default:
                return "建议根据具体情况采取相应措施";
        }
    }
}
