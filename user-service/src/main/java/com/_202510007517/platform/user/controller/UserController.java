package com._202510007517.platform.user.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping("/me")
    public ResponseResult<UserProfileDTO> getCurrentUser(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(userApplicationService.getProfile(resolveCurrentUserId(userIdHeader)), "获取用户成功", 200);
    }

    @GetMapping("/{userId}")
    public ResponseResult<UserProfileDTO> getUserById(@PathVariable Long userId) {
        return ResponseResult.success(userApplicationService.getProfile(userId), "获取用户成功", 200);
    }

    @PutMapping("/me")
    public ResponseResult<UserProfileDTO> updateCurrentUser(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody UpdateUserProfileDTO request) {
        return ResponseResult.success(userApplicationService.updateProfile(resolveCurrentUserId(userIdHeader), request), "用户信息更新成功", 200);
    }

    @PutMapping("/{userId}")
    public ResponseResult<UserProfileDTO> updateUserById(@PathVariable Long userId,
                                                         @RequestBody UpdateUserProfileDTO request) {
        return ResponseResult.success(userApplicationService.updateProfile(userId, request), "用户信息更新成功", 200);
    }

    @GetMapping("/students/{studentId}")
    public ResponseResult<StudentProfileDTO> getStudentById(@PathVariable Long studentId) {
        return ResponseResult.success(userApplicationService.getStudentProfile(studentId), "获取学生详情成功", 200);
    }

    @PutMapping("/students/{studentId}")
    public ResponseResult<StudentProfileDTO> updateStudentById(@PathVariable Long studentId,
                                                               @RequestBody UpdateStudentProfileDTO request) {
        return ResponseResult.success(userApplicationService.updateStudentProfile(studentId, request), "学生信息更新成功", 200);
    }

    private static Long resolveCurrentUserId(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        throw new IllegalArgumentException("缺少用户身份");
    }
}
