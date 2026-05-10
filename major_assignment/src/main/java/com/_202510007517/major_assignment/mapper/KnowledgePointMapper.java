package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgePointMapper {

    // 根据课程ID获取知识点列表
    @Select("SELECT id, point_name AS pointName, description, difficulty, order_index AS orderIndex, course_id AS courseId FROM knowledge_points WHERE course_id = #{courseId} ORDER BY order_index")
    List<Map<String, Object>> getKnowledgePointsByCourseId(@Param("courseId") Long courseId);

    // 根据课程ID获取知识点实体列表
    @Select("SELECT id, point_name AS pointName, description, difficulty, order_index AS orderIndex, course_id AS courseId FROM knowledge_points WHERE course_id = #{courseId} ORDER BY order_index")
    List<KnowledgePoint> findByCourseId(@Param("courseId") Long courseId);

    // 根据多个课程ID获取知识点实体列表（foreach）→ 见 KnowledgePointMapper.xml
    List<KnowledgePoint> findByCourseIds(@Param("courseIds") List<Long> courseIds);

    // 获取课程的平均掌握率（多表 JOIN + CASE + 动态 WHERE）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getAverageMasteryRatesByCourse(@Param("courseId") Long courseId,
                                                             @Param("classId") Long classId,
                                                             @Param("studentId") Long studentId,
                                                             @Param("knowledgePointId") Long knowledgePointId);

    // 获取知识点统计数据（多表 JOIN + 动态 WHERE）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getKnowledgePointStats(@Param("classId") Long classId,
                                                     @Param("studentId") Long studentId,
                                                     @Param("knowledgePointId") Long knowledgePointId);

    // 获取需要关注的学生ID列表（多表 JOIN + 动态 WHERE）→ 见 KnowledgePointMapper.xml
    List<Long> getAtRiskStudentIds(@Param("courseId") Long courseId,
                                   @Param("classId") Long classId,
                                   @Param("studentId") Long studentId,
                                   @Param("knowledgePointId") Long knowledgePointId);

    // 根据ID获取知识点
    @Select("SELECT id, point_name AS pointName, description, difficulty, order_index AS orderIndex, course_id AS courseId FROM knowledge_points WHERE id = #{id}")
    Map<String, Object> getKnowledgePointById(@Param("id") Long id);

    // 根据ID获取知识点实体
    @Select("SELECT id, point_name AS pointName, description, difficulty, order_index AS orderIndex, course_id AS courseId FROM knowledge_points WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "pointName", column = "pointName"),
        @Result(property = "description", column = "description"),
        @Result(property = "difficulty", column = "difficulty"),
        @Result(property = "orderIndex", column = "orderIndex"),
        @Result(property = "courseId", column = "courseId")
    })
    KnowledgePoint findById(@Param("id") Long id);

    // 插入知识点
    @Insert("INSERT INTO knowledge_points (point_name, description, difficulty, order_index, course_id) " +
            "VALUES (#{pointName}, #{description}, #{difficulty}, #{orderIndex}, #{courseId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertKnowledgePoint(KnowledgePoint knowledgePoint);

    // 更新知识点
    @Update("UPDATE knowledge_points SET point_name = #{pointName}, description = #{description}, " +
            "difficulty = #{difficulty}, order_index = #{orderIndex}, course_id = #{courseId} WHERE id = #{id}")
    void updateKnowledgePoint(KnowledgePoint knowledgePoint);

    // 删除知识点
    @Delete("DELETE FROM knowledge_points WHERE id = #{id}")
    void deleteKnowledgePoint(@Param("id") Long id);

    // 获取作业关联的知识点ID列表
    @Select("SELECT knowledge_point_id FROM assignment_knowledge_points WHERE assignment_id = #{assignmentId}")
    List<Long> getKnowledgePointIdsByAssignmentId(@Param("assignmentId") Long assignmentId);

    // 获取考试关联的知识点ID列表
    @Select("SELECT knowledge_point_id FROM exam_knowledge_points WHERE exam_id = #{examId}")
    List<Long> getKnowledgePointIdsByExamId(@Param("examId") Long examId);

    // 为作业添加知识点关联
    @Insert("INSERT IGNORE INTO assignment_knowledge_points (assignment_id, knowledge_point_id) VALUES (#{assignmentId}, #{knowledgePointId})")
    void addAssignmentKnowledgePoint(@Param("assignmentId") Long assignmentId, @Param("knowledgePointId") Long knowledgePointId);

    // 删除作业的所有知识点关联
    @Delete("DELETE FROM assignment_knowledge_points WHERE assignment_id = #{assignmentId}")
    void deleteAssignmentKnowledgePoints(@Param("assignmentId") Long assignmentId);

    // 为考试添加知识点关联
    @Insert("INSERT IGNORE INTO exam_knowledge_points (exam_id, knowledge_point_id) VALUES (#{examId}, #{knowledgePointId})")
    void addExamKnowledgePoint(@Param("examId") Long examId, @Param("knowledgePointId") Long knowledgePointId);

    // 删除考试的所有知识点关联
    @Delete("DELETE FROM exam_knowledge_points WHERE exam_id = #{examId}")
    void deleteExamKnowledgePoints(@Param("examId") Long examId);

    // 获取作业关联的知识点详情列表（多表 JOIN）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getKnowledgePointsByAssignmentId(@Param("assignmentId") Long assignmentId);

    // 获取考试关联的知识点详情列表（多表 JOIN）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getKnowledgePointsByExamId(@Param("examId") Long examId);

    // 知识点相关作业成绩（多表 JOIN）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getAssignmentScoresByKnowledgePointAndStudent(@Param("knowledgePointId") Long knowledgePointId, @Param("studentId") Long studentId);

    // 知识点相关考试成绩（多表 JOIN）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getExamScoresByKnowledgePointAndStudent(@Param("knowledgePointId") Long knowledgePointId, @Param("studentId") Long studentId);

    // 更新或插入学生知识点掌握情况（单表 UPSERT，≤ 10 行）→ 保留注解
    @Insert({
        "INSERT INTO knowledge_mastery (student_id, knowledge_point_id, mastery_level, last_assessed_date)",
        "VALUES (#{studentId}, #{knowledgePointId}, #{masteryLevel}, NOW())",
        "ON DUPLICATE KEY UPDATE mastery_level = #{masteryLevel}, update_time = NOW()"
    })
    void upsertKnowledgeMastery(@Param("studentId") Long studentId, @Param("knowledgePointId") Long knowledgePointId, @Param("masteryLevel") String masteryLevel);

    // 学生的知识点掌握情况（多表 JOIN）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getStudentKnowledgeMastery(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    // 课程下所有知识点的学生掌握情况统计（多表 LEFT JOIN + CASE）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getKnowledgePointMasteryStats(@Param("courseId") Long courseId);

    // 获取班级下的所有学生ID列表
    @Select("SELECT DISTINCT student_id FROM class_students WHERE class_id = #{classId}")
    List<Long> getStudentIdsByClassId(@Param("classId") Long classId);

    // 获取课程班级下的所有学生ID列表（UNION 子查询）→ 见 KnowledgePointMapper.xml
    List<Long> getStudentIdsByCourseId(@Param("courseId") Long courseId);

    // 获取教师所有课程班级下的学生ID列表（UNION 子查询）→ 见 KnowledgePointMapper.xml
    List<Long> getStudentIdsByTeacherId(@Param("teacherId") Long teacherId);

    // 学生在指定知识点的掌握情况（CASE + foreach）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getStudentMasteryByKnowledgePoints(@Param("knowledgePointIds") List<Long> knowledgePointIds,
                                                                  @Param("studentIds") List<Long> studentIds);

    // 学生信息批量查询（foreach）→ 见 KnowledgePointMapper.xml
    List<Map<String, Object>> getStudentInfoByIds(@Param("studentIds") List<Long> studentIds);
}
