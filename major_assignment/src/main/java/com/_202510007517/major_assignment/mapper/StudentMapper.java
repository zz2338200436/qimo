package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {
    List<Course> getStudentCourses(Long studentId);
    
    /**
     * 带筛选和分页的学生课程查询
     */
    List<Course> getStudentCoursesWithFilters(@Param("studentId") Long studentId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit,
                                              @Param("sortBy") String sortBy,
                                              @Param("order") String order,
                                              @Param("courseStatus") String courseStatus,
                                              @Param("semester") String semester,
                                              @Param("courseCategory") String courseCategory,
                                              @Param("searchQuery") String searchQuery);
    
    /**
     * 统计带筛选条件的学生课程数量
     */
    int countStudentCoursesWithFilters(@Param("studentId") Long studentId,
                                       @Param("courseStatus") String courseStatus,
                                       @Param("semester") String semester,
                                       @Param("courseCategory") String courseCategory,
                                       @Param("searchQuery") String searchQuery);
    Map<String, Object> getLearningStats(@Param("studentId") Long studentId,
                                         @Param("semester") String semester,
                                         @Param("courseId") Long courseId,
                                         @Param("timeRange") String timeRange);
    List<Map<String, Object>> getKnowledgePoints(@Param("studentId") Long studentId,
                                                  @Param("semester") String semester,
                                                  @Param("courseId") Long courseId,
                                                  @Param("timeRange") String timeRange);
    Map<String, Object> getStudentPerformance(Long studentId);
    List<Map<String, Object>> getScores(@Param("studentId") Long studentId,
                                         @Param("semester") String semester,
                                         @Param("courseId") Long courseId,
                                         @Param("timeRange") String timeRange);
    List<Map<String, Object>> getLearningProgressTrend(Long studentId);
    
    @Select("SELECT u.id, u.username, u.name, u.email, u.phone, u.avatar FROM users u JOIN class_students cs ON u.id = cs.student_id JOIN user_roles ur ON u.id = ur.user_id WHERE cs.class_id = #{classId} AND ur.role_id = 3")
    List<Map<String, Object>> getStudentsByClassId(Long classId);
    
    // 获取学生学习时间分布
    List<Map<String, Object>> getStudyTimeDistribution(@Param("studentId") Long studentId,
                                                         @Param("type") String type,
                                                         @Param("semester") String semester,
                                                         @Param("courseId") Long courseId,
                                                         @Param("timeRange") String timeRange);
    
    // 更新学生学习表现数据
    void updateStudentPerformance(@Param("studentId") Long studentId, @Param("averageScore") Double averageScore, @Param("pendingAssignments") Integer pendingAssignments, @Param("overallProgress") Integer overallProgress);
    
    // 更新学生班级信息（旧方法，保持兼容）
    void updateStudentClass(@Param("studentId") Long studentId, @Param("classId") Long classId);
    
    // 删除学生旧的班级关联
    void deleteStudentClass(@Param("studentId") Long studentId);
    
    // 插入学生新的班级关联
    void insertStudentClass(@Param("classId") Long classId, @Param("studentId") Long studentId);
    
    // 获取课程进度
    Integer getCourseProgress(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    // 获取学生完整信息（包括专业、年级、班级）
    @Select("SELECT cc.class_name as className, cc.year as grade, m.major_name as major " +
            "FROM class_students cs " +
            "JOIN course_classes cc ON cs.class_id = cc.id " +
            "LEFT JOIN majors m ON cc.major_id = m.id " +
            "WHERE cs.student_id = #{studentId} " +
            "LIMIT 1")
    Map<String, Object> getStudentClassInfo(@Param("studentId") Long studentId);
    
    // 学情预警分析相关方法
    
    /**
     * 获取所有活跃的学生-课程组合
     */
    List<Map<String, Object>> getAllActiveStudentCourses();
    
    /**
     * 获取学生最近的成绩数据
     */
    List<Map<String, Object>> getRecentScores(@Param("studentId") Long studentId, 
                                              @Param("courseId") Long courseId, 
                                              @Param("days") int days);
    
    /**
     * 获取学生学习进度数据
     */
    Map<String, Object> getStudentProgressData(@Param("studentId") Long studentId, 
                                               @Param("courseId") Long courseId);
    
    /**
     * 获取知识点相关的成绩数据
     */
    List<Map<String, Object>> getKnowledgePointRelatedScores(@Param("studentId") Long studentId, 
                                                              @Param("knowledgePointId") Long knowledgePointId);
    
    /**
     * 更新知识点掌握情况
     */
    void updateKnowledgePointMastery(@Param("studentId") Long studentId, 
                                     @Param("knowledgePointId") Long knowledgePointId, 
                                     @Param("masteryData") Map<String, Object> masteryData);
}