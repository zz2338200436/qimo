package com._202510007517.platform.user.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentCompatibilityController {

    private final UserApplicationService userApplicationService;

    public StudentCompatibilityController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping("/student/profile")
    public ResponseResult<Map<String, Object>> getProfile(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        StudentProfileDTO profile = userApplicationService.getStudentProfile(resolveCurrentUserId(userIdHeader));
        return ResponseResult.success(toLegacyProfile(profile), "获取用户信息成功", 200);
    }

    @PutMapping("/student/profile")
    public ResponseResult<Boolean> updateProfile(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> request) {
        UpdateStudentProfileDTO updateRequest = new UpdateStudentProfileDTO();
        updateRequest.setRealName(stringValue(request, "name"));
        updateRequest.setEmail(stringValue(request, "email"));
        updateRequest.setPhone(stringValue(request, "phone"));
        updateRequest.setAvatar(stringValue(request, "avatar"));
        userApplicationService.updateStudentProfile(resolveCurrentUserId(userIdHeader), updateRequest);
        return ResponseResult.success(true, "个人资料更新成功", 200);
    }

    @GetMapping("/student/notification-settings")
    public ResponseResult<Map<String, Object>> getNotificationSettings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(userApplicationService.getNotificationSettings(resolveCurrentUserId(userIdHeader)),
                "获取通知设置成功", 200);
    }

    @PutMapping("/student/notification-settings")
    public ResponseResult<Boolean> updateNotificationSettings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> request) {
        return ResponseResult.success(userApplicationService.updateNotificationSettings(resolveCurrentUserId(userIdHeader), request),
                "通知设置更新成功", 200);
    }

    @GetMapping("/student/privacy-settings")
    public ResponseResult<Map<String, Object>> getPrivacySettings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(userApplicationService.getPrivacySettings(resolveCurrentUserId(userIdHeader)),
                "获取隐私设置成功", 200);
    }

    @PutMapping("/student/privacy-settings")
    public ResponseResult<Boolean> updatePrivacySettings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> request) {
        return ResponseResult.success(userApplicationService.updatePrivacySettings(resolveCurrentUserId(userIdHeader), request),
                "隐私设置更新成功", 200);
    }

    @PostMapping("/student/upload-avatar")
    public ResponseResult<Boolean> uploadAvatar(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> request) {
        return ResponseResult.success(userApplicationService.uploadAvatar(resolveCurrentUserId(userIdHeader), stringValue(request, "avatar")),
                "头像上传成功", 200);
    }

    @GetMapping("/student/export-data")
    public ResponseResult<Map<String, Object>> exportData(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(userApplicationService.exportStudentData(resolveCurrentUserId(userIdHeader)),
                "数据导出成功", 200);
    }

    @GetMapping("/students/{studentId}/class")
    public ResponseResult<String> getStudentClassName(@PathVariable Long studentId) {
        return ResponseResult.success(userApplicationService.getStudentClassName(studentId),
                "获取班级名称成功", 200);
    }

    private static Map<String, Object> toLegacyProfile(StudentProfileDTO profile) {
        Map<String, Object> legacyProfile = new LinkedHashMap<>();
        legacyProfile.put("id", profile.getStudentId());
        legacyProfile.put("studentId", profile.getStudentId());
        legacyProfile.put("username", profile.getUsername());
        legacyProfile.put("name", profile.getRealName());
        legacyProfile.put("realName", profile.getRealName());
        legacyProfile.put("email", profile.getEmail());
        legacyProfile.put("phone", profile.getPhone());
        legacyProfile.put("avatar", profile.getAvatar());
        legacyProfile.put("className", profile.getClassName());
        legacyProfile.put("roles", profile.getRoles());
        return legacyProfile;
    }

    private static String stringValue(Map<String, Object> request, String key) {
        if (request == null || request.get(key) == null) {
            return null;
        }
        return String.valueOf(request.get(key));
    }

    private static Long resolveCurrentUserId(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        throw new IllegalArgumentException("缺少学生身份");
    }
}
