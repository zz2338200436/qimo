package com._202510007517.platform.notification.service;

import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCommandServiceTest {

    @Test
    void sendNotificationBuildsUnreadNotificationForStudent() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);
        TeacherSendNotificationRequestDTO request = new TeacherSendNotificationRequestDTO();
        request.setType("course");
        request.setTitle("开课通知");
        request.setContent("请同学按时参加第一节课");
        request.setStudentId(42L);
        request.setRelatedId(null);
        request.setIsRead(false);

        when(repository.save(org.mockito.ArgumentMatchers.any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, NotificationEntity.class));

        NotificationEntity saved = service.sendNotification(7L, request);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(repository).save(captor.capture());
        NotificationEntity toSave = captor.getValue();
        assertThat(toSave.getTeacherId()).isEqualTo(7L);
        assertThat(toSave.getStudentId()).isEqualTo(42L);
        assertThat(toSave.getType()).isEqualTo("course");
        assertThat(toSave.getTitle()).isEqualTo("开课通知");
        assertThat(toSave.getContent()).contains("第一节课");
        assertThat(toSave.getRead()).isFalse();
        assertThat(saved.getTeacherId()).isEqualTo(7L);
        assertThat(saved.getStudentId()).isEqualTo(42L);
    }

    @Test
    void sendBatchNotificationsBuildsUnreadNotificationsForEachStudent() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);
        TeacherSendNotificationRequestDTO first = new TeacherSendNotificationRequestDTO();
        first.setType("course");
        first.setTitle("开课通知");
        first.setContent("第一位同学通知");
        first.setStudentId(42L);
        first.setRelatedId(5001L);
        first.setIsRead(false);
        TeacherSendNotificationRequestDTO second = new TeacherSendNotificationRequestDTO();
        second.setType("course");
        second.setTitle("开课通知");
        second.setContent("第二位同学通知");
        second.setStudentId(43L);
        second.setRelatedId(5001L);
        second.setIsRead(false);

        when(repository.save(org.mockito.ArgumentMatchers.any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, NotificationEntity.class));

        List<NotificationEntity> saved = service.sendBatchNotifications(7L, List.of(first, second));

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationEntity::getTeacherId)
                .containsExactly(7L, 7L);
        assertThat(captor.getAllValues())
                .extracting(NotificationEntity::getStudentId)
                .containsExactly(42L, 43L);
        assertThat(captor.getAllValues())
                .extracting(NotificationEntity::getRead)
                .containsExactly(false, false);
        assertThat(saved)
                .extracting(NotificationEntity::getStudentId)
                .containsExactly(42L, 43L);
    }

    @Test
    void markNotificationAsReadDelegatesToRepository() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);

        service.markNotificationAsRead(42L, 9L);

        verify(repository).markAsRead(42L, 9L);
    }

    @Test
    void markAllNotificationsAsReadDelegatesToRepository() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);

        service.markAllNotificationsAsRead(42L);

        verify(repository).markAllAsRead(42L);
    }

    @Test
    void deleteNotificationDelegatesToRepository() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);

        service.deleteNotification(42L, 9L);

        verify(repository).deleteByIdAndStudentId(42L, 9L);
    }

    @Test
    void deleteAllReadNotificationsDelegatesToRepository() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationCommandService service = new NotificationCommandService(repository);

        service.deleteAllReadNotifications(42L);

        verify(repository).deleteAllReadByStudentId(42L);
    }
}
