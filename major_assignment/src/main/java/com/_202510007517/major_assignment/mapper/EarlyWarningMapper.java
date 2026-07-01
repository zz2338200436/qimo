package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.EarlyWarning;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface EarlyWarningMapper {
    // 多表 JOIN → XML（见 EarlyWarningMapper.xml）
    List<EarlyWarning> findUnresolvedByTeacherId(Long teacherId);

    @Select("SELECT COUNT(*) FROM early_warnings WHERE teacher_id = #{teacherId}")
    Long countTotalWarningsByTeacherId(Long teacherId);

    @Select("SELECT COUNT(*) FROM early_warnings WHERE teacher_id = #{teacherId} AND is_resolved = 0")
    Long countPendingWarningsByTeacherId(Long teacherId);

    @Select("SELECT COUNT(*) FROM early_warnings WHERE teacher_id = #{teacherId} AND warning_type = #{warningType}")
    Long countWarningsByType(Long teacherId, String warningType);

    // 带筛选条件的统计方法：动态 JOIN/WHERE → XML
    Long countTotalWarningsWithFilter(@Param("teacherId") Long teacherId,
                                      @Param("classId") Long classId,
                                      @Param("courseId") Long courseId);

    Long countPendingWarningsWithFilter(@Param("teacherId") Long teacherId,
                                        @Param("classId") Long classId,
                                        @Param("courseId") Long courseId);

    Long countWarningsByTypeWithFilter(@Param("teacherId") Long teacherId,
                                       @Param("warningType") String warningType,
                                       @Param("classId") Long classId,
                                       @Param("courseId") Long courseId);

    // 使用XML映射文件替代注解方式
    List<EarlyWarning> findWarningsByCondition(Long teacherId, Long classId, Long courseId,
                                             String warningType, String status,
                                             Integer offset, Integer size);

    Long countWarningsByCondition(Long teacherId, Long classId, Long courseId,
                                String warningType, String status);

    EarlyWarning findWarningById(Long warningId);

    boolean updateWarningStatus(@Param("warningId") Long warningId,
                              @Param("updateDTO") EarlyWarning updateDTO,
                              @Param("userId") Long userId);

    List<EarlyWarning> findWarningsForExport(Long teacherId, Long classId, Long courseId,
                                           String warningType, String status);

    @Insert("INSERT INTO early_warnings (student_id, course_id, teacher_id, warning_type, warning_level, warning_message, assessment_type, related_assessment_id, trigger_date, is_resolved) VALUES (#{studentId}, #{courseId}, #{teacherId}, #{warningType}, #{warningLevel}, #{warningMessage}, #{assessmentType}, #{relatedAssessmentId}, NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EarlyWarning earlyWarning);

    @Delete("DELETE FROM early_warnings WHERE id = #{id}")
    int deleteById(Long id);

    List<EarlyWarning> findByCourseId(Long courseId, String warningType, String warningLevel, Boolean isResolved);

    /**
     * 获取教师最近的学情预警记录（用于仪表盘最近活动）
     */
    @Select("SELECT ew.*, u.name AS studentName FROM early_warnings ew LEFT JOIN users u ON ew.student_id = u.id WHERE ew.teacher_id = #{teacherId} ORDER BY ew.trigger_date DESC LIMIT #{limit}")
    List<EarlyWarning> findRecentByTeacherId(@Param("teacherId") Long teacherId, @Param("limit") int limit);
}
