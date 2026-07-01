package com._202510007517.platform.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
            SELECT n
            FROM NotificationEntity n
            WHERE n.studentId = :studentId
              AND (
                  :filter = 'all'
                  OR (:filter = 'read' AND n.read = true)
                  OR (:filter = 'unread' AND n.read = false)
                  OR n.type = :filter
              )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<NotificationEntity> findStudentNotifications(@Param("studentId") Long studentId,
                                                       @Param("filter") String filter,
                                                       Pageable pageable);

    @Query("""
            SELECT COUNT(n)
            FROM NotificationEntity n
            WHERE n.studentId = :studentId
              AND (
                  :filter = 'all'
                  OR (:filter = 'read' AND n.read = true)
                  OR (:filter = 'unread' AND n.read = false)
                  OR n.type = :filter
              )
            """)
    long countStudentNotifications(@Param("studentId") Long studentId, @Param("filter") String filter);

    @Query("""
            SELECT n
            FROM NotificationEntity n
            WHERE n.studentId = :studentId
              AND (
                  :filter = 'all'
                  OR (:filter = 'read' AND n.read = true)
                  OR (:filter = 'unread' AND n.read = false)
                  OR n.type = :filter
              )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<NotificationEntity> findAllStudentNotifications(@Param("studentId") Long studentId,
                                                         @Param("filter") String filter);

    @Query("""
            SELECT n
            FROM NotificationEntity n
            WHERE n.teacherId = :teacherId
              AND (
                  :filter = 'all'
                  OR n.type = :filter
              )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<NotificationEntity> findTeacherSentNotifications(@Param("teacherId") Long teacherId,
                                                          @Param("filter") String filter,
                                                          Pageable pageable);

    @Query("""
            SELECT COUNT(n)
            FROM NotificationEntity n
            WHERE n.teacherId = :teacherId
              AND (
                  :filter = 'all'
                  OR n.type = :filter
              )
            """)
    long countTeacherSentNotifications(@Param("teacherId") Long teacherId, @Param("filter") String filter);

    int countByStudentIdAndReadFalse(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationEntity n
            SET n.read = true
            WHERE n.id = :notificationId
              AND n.studentId = :studentId
            """)
    int markAsRead(@Param("studentId") Long studentId, @Param("notificationId") Long notificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationEntity n
            SET n.read = true
            WHERE n.studentId = :studentId
              AND n.read = false
            """)
    int markAllAsRead(@Param("studentId") Long studentId);

    int deleteByIdAndStudentId(Long notificationId, Long studentId);

    int deleteByStudentIdAndReadTrue(Long studentId);
}
