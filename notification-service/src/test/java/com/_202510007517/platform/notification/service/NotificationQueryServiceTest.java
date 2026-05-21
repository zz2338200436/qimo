package com._202510007517.platform.notification.service;

import com._202510007517.platform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationQueryServiceTest {

    @Test
    void getStudentNotificationsIncludesTotalPagesForLegacyFrontendCompatibility() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationQueryService service = new NotificationQueryService(repository);
        when(repository.findStudentNotifications(42L, 0, 10, "all")).thenReturn(List.of());
        when(repository.countStudentNotifications(42L, "all")).thenReturn(21L);

        Map<String, Object> result = service.getStudentNotifications(42L, 1, 10, "all");

        assertThat(result).containsEntry("total", 21L);
        assertThat(result).containsEntry("page", 1);
        assertThat(result).containsEntry("size", 10);
        assertThat(result).containsEntry("totalPages", 3);
    }
}
