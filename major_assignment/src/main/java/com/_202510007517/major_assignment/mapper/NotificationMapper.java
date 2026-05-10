package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper {
    // 根据学生ID获取所有通知
    @Select("SELECT id, student_id as studentId, teacher_id as teacherId, type, title, content, related_id as relatedId, is_read as isRead, created_at as createdAt FROM notifications WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<Notification> getNotificationsByStudentId(Long studentId);

    // 根据学生ID获取未读通知数量
    @Select("SELECT COUNT(*) FROM notifications WHERE student_id = #{studentId} AND is_read = false")
    Integer getUnreadNotificationCountByStudentId(Long studentId);

    // 根据ID获取通知
    @Select("SELECT id, student_id as studentId, teacher_id as teacherId, type, title, content, related_id as relatedId, is_read as isRead, created_at as createdAt FROM notifications WHERE id = #{id}")
    Notification getNotificationById(Long id);

    // 新增通知
    @Insert("INSERT INTO notifications(student_id, teacher_id, type, title, content, related_id, is_read, created_at) VALUES(#{studentId}, #{teacherId}, #{type}, #{title}, #{content}, #{relatedId}, #{isRead}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Notification notification);

    // 更新通知为已读
    @Update("UPDATE notifications SET is_read = true WHERE id = #{id}")
    void markAsRead(Long id);

    // 将所有通知标记为已读
    @Update("UPDATE notifications SET is_read = true WHERE student_id = #{studentId}")
    void markAllAsRead(Long studentId);

    // 删除通知
    @Delete("DELETE FROM notifications WHERE id = #{id}")
    void delete(Long id);

    // 删除所有已读通知
    @Delete("DELETE FROM notifications WHERE student_id = #{studentId} AND is_read = true")
    void deleteAllRead(Long studentId);

    // 批量创建通知
    void insertBatch(@Param("notifications") List<Notification> notifications);
}