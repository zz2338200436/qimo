package com._202510007517.platform.user.service;

import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import com._202510007517.platform.user.domain.UserRecord;
import com._202510007517.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserApplicationService {

    private final UserRepository userRepository;

    public UserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileDTO getProfile(Long userId) {
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toDto(user, userRepository.findRolesByUserId(userId));
    }

    public UserProfileDTO getProfileByUsername(String username) {
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toDto(user, userRepository.findRolesByUserId(user.getId()));
    }

    public UserProfileDTO updateProfile(Long userId, UpdateUserProfileDTO request) {
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        if (request != null) {
            if (request.getName() != null) {
                user.setName(request.getName());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }
            if (request.getAvatar() != null) {
                user.setAvatar(request.getAvatar());
            }
        }
        userRepository.updateProfile(user);
        return toDto(user, userRepository.findRolesByUserId(user.getId()));
    }

    public List<UserProfileDTO> listByIds(List<Long> ids) {
        return userRepository.findByIds(ids).stream()
                .map(user -> toDto(user, userRepository.findRolesByUserId(user.getId())))
                .toList();
    }

    public List<String> getRoles(Long userId) {
        ensureExists(userId);
        return userRepository.findRolesByUserId(userId);
    }

    public UserRolesDTO getRolesDetail(Long userId) {
        UserRolesDTO dto = new UserRolesDTO();
        dto.setUserId(userId);
        dto.setRoles(getRoles(userId));
        return dto;
    }

    public StudentProfileDTO getStudentProfile(Long studentId) {
        UserRecord user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toStudentProfileDto(user);
    }

    public StudentProfileDTO updateStudentProfile(Long studentId, UpdateStudentProfileDTO request) {
        UserRecord user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        if (request != null) {
            if (request.getRealName() != null) {
                user.setName(request.getRealName());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }
            if (request.getAvatar() != null) {
                user.setAvatar(request.getAvatar());
            }
        }
        userRepository.updateProfile(user);
        return toStudentProfileDto(user);
    }

    public Map<String, Object> getNotificationSettings(Long studentId) {
        ensureExists(studentId);
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("emailNotifications", false);
        settings.put("smsNotifications", false);
        settings.put("webNotifications", false);
        settings.put("assignmentNotifications", false);
        settings.put("gradeNotifications", false);
        settings.put("courseNotifications", false);
        settings.put("systemNotifications", false);
        settings.put("reminderNotifications", false);
        return settings;
    }

    public boolean updateNotificationSettings(Long studentId, Map<String, Object> settings) {
        ensureExists(studentId);
        return true;
    }

    public Map<String, Object> getPrivacySettings(Long studentId) {
        ensureExists(studentId);
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("shareProfile", false);
        settings.put("shareAchievements", false);
        settings.put("dataCollection", false);
        return settings;
    }

    public boolean updatePrivacySettings(Long studentId, Map<String, Object> settings) {
        ensureExists(studentId);
        return true;
    }

    public boolean uploadAvatar(Long studentId, String avatarUrl) {
        UpdateStudentProfileDTO request = new UpdateStudentProfileDTO();
        request.setAvatar(avatarUrl);
        updateStudentProfile(studentId, request);
        return true;
    }

    public String getStudentClassName(Long studentId) {
        ensureExists(studentId);
        return userRepository.findStudentClassName(studentId);
    }

    public Map<String, Object> exportStudentData(Long studentId) {
        StudentProfileDTO profile = getStudentProfile(studentId);
        Map<String, Object> basicInfo = new LinkedHashMap<>();
        basicInfo.put("id", profile.getStudentId());
        basicInfo.put("studentId", profile.getStudentId());
        basicInfo.put("username", profile.getUsername());
        basicInfo.put("name", profile.getRealName());
        basicInfo.put("realName", profile.getRealName());
        basicInfo.put("email", profile.getEmail());
        basicInfo.put("phone", profile.getPhone());
        basicInfo.put("avatar", profile.getAvatar());
        basicInfo.put("className", profile.getClassName());

        Map<String, Object> exportedData = new LinkedHashMap<>();
        exportedData.put("basicInfo", basicInfo);
        exportedData.put("courses", List.of());
        exportedData.put("learningStats", Map.of());
        exportedData.put("scores", List.of());
        return exportedData;
    }

    private void ensureExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private UserProfileDTO toDto(UserRecord user, List<String> roles) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoles(roles);
        return dto;
    }

    private StudentProfileDTO toStudentProfileDto(UserRecord user) {
        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setStudentId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setClassName(userRepository.findStudentClassName(user.getId()));
        dto.setRoles(userRepository.findRolesByUserId(user.getId()));
        return dto;
    }
}
