package com._202510007517.platform.user.service;

import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.domain.UserRecord;
import com._202510007517.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserApplicationServiceTest {

    @Test
    void getStudentProfileCombinesUserRolesAndClassName() {
        UserApplicationService service = new UserApplicationService(new FakeUserRepository());

        StudentProfileDTO profile = service.getStudentProfile(42L);

        assertThat(profile.getStudentId()).isEqualTo(42L);
        assertThat(profile.getUsername()).isEqualTo("student42");
        assertThat(profile.getRealName()).isEqualTo("学生四二");
        assertThat(profile.getClassName()).isEqualTo("计科 2301");
        assertThat(profile.getRoles()).containsExactly("STUDENT");
    }

    @Test
    void getStudentProfileFailsWhenUserDoesNotExist() {
        UserApplicationService service = new UserApplicationService(new FakeUserRepository());

        assertThatThrownBy(() -> service.getStudentProfile(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void updateStudentProfileUpdatesOnlyProvidedFields() {
        FakeUserRepository repository = new FakeUserRepository();
        UserApplicationService service = new UserApplicationService(repository);
        UpdateStudentProfileDTO request = new UpdateStudentProfileDTO();
        request.setRealName("新名字");
        request.setEmail("new42@example.com");

        StudentProfileDTO profile = service.updateStudentProfile(42L, request);

        assertThat(profile.getRealName()).isEqualTo("新名字");
        assertThat(profile.getEmail()).isEqualTo("new42@example.com");
        assertThat(profile.getPhone()).isEqualTo("13800000042");
        assertThat(repository.updatedUser).isNotNull();
        assertThat(repository.updatedUser.getName()).isEqualTo("新名字");
        assertThat(repository.updatedUser.getEmail()).isEqualTo("new42@example.com");
        assertThat(repository.updatedUser.getPhone()).isEqualTo("13800000042");
    }

    @Test
    void updateUserProfileUpdatesOnlyProvidedFields() {
        FakeUserRepository repository = new FakeUserRepository();
        UserApplicationService service = new UserApplicationService(repository);
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setName("普通用户新名");
        request.setPhone("13900000042");

        UserProfileDTO profile = service.updateProfile(42L, request);

        assertThat(profile.getId()).isEqualTo(42L);
        assertThat(profile.getName()).isEqualTo("普通用户新名");
        assertThat(profile.getEmail()).isEqualTo("student42@example.com");
        assertThat(profile.getPhone()).isEqualTo("13900000042");
        assertThat(repository.updatedUser).isNotNull();
        assertThat(repository.updatedUser.getName()).isEqualTo("普通用户新名");
        assertThat(repository.updatedUser.getEmail()).isEqualTo("student42@example.com");
        assertThat(repository.updatedUser.getPhone()).isEqualTo("13900000042");
    }

    @Test
    void legacySettingsReturnStableDefaultValuesAfterUserExists() {
        UserApplicationService service = new UserApplicationService(new FakeUserRepository());

        Map<String, Object> notificationSettings = service.getNotificationSettings(42L);
        Map<String, Object> privacySettings = service.getPrivacySettings(42L);

        assertThat(notificationSettings)
                .containsEntry("emailNotifications", false)
                .containsEntry("smsNotifications", false)
                .containsEntry("webNotifications", false)
                .containsEntry("assignmentNotifications", false)
                .containsEntry("gradeNotifications", false)
                .containsEntry("courseNotifications", false)
                .containsEntry("systemNotifications", false)
                .containsEntry("reminderNotifications", false);
        assertThat(privacySettings)
                .containsEntry("shareProfile", false)
                .containsEntry("shareAchievements", false)
                .containsEntry("dataCollection", false);
    }

    @Test
    void legacyUpdateSettingsOnlyRequiresExistingStudent() {
        UserApplicationService service = new UserApplicationService(new FakeUserRepository());

        assertThat(service.updateNotificationSettings(42L, Map.of("emailNotifications", true))).isTrue();
        assertThat(service.updatePrivacySettings(42L, Map.of("shareProfile", true))).isTrue();
        assertThatThrownBy(() -> service.updateNotificationSettings(404L, Map.of()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void uploadAvatarDelegatesToStudentProfileUpdate() {
        FakeUserRepository repository = new FakeUserRepository();
        UserApplicationService service = new UserApplicationService(repository);

        assertThat(service.uploadAvatar(42L, "/avatars/new.png")).isTrue();

        assertThat(repository.updatedUser).isNotNull();
        assertThat(repository.updatedUser.getAvatar()).isEqualTo("/avatars/new.png");
    }

    @Test
    void exportStudentDataKeepsLegacySections() {
        UserApplicationService service = new UserApplicationService(new FakeUserRepository());

        Map<String, Object> exportedData = service.exportStudentData(42L);

        assertThat(exportedData).containsKeys("basicInfo", "courses", "learningStats", "scores");
        assertThat(exportedData.get("basicInfo"))
                .isInstanceOfSatisfying(Map.class, basicInfo -> {
                    assertThat(basicInfo).containsEntry("id", 42L);
                    assertThat(basicInfo).containsEntry("name", "学生四二");
                    assertThat(basicInfo).containsEntry("className", "计科 2301");
                });
    }

    private static class FakeUserRepository implements UserRepository {
        private UserRecord updatedUser;

        @Override
        public Optional<UserRecord> findById(Long id) {
            if (!Long.valueOf(42L).equals(id)) {
                return Optional.empty();
            }
            UserRecord user = new UserRecord();
            user.setId(42L);
            user.setUsername("student42");
            user.setName("学生四二");
            user.setEmail("student42@example.com");
            user.setPhone("13800000042");
            user.setAvatar("/avatars/42.png");
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return Optional.of(user);
        }

        @Override
        public Optional<UserRecord> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public List<UserRecord> findByIds(List<Long> ids) {
            return List.of();
        }

        @Override
        public List<String> findRolesByUserId(Long userId) {
            return List.of("STUDENT");
        }

        @Override
        public String findStudentClassName(Long studentId) {
            return "计科 2301";
        }

        @Override
        public void updateProfile(UserRecord user) {
            updatedUser = user;
        }
    }
}
