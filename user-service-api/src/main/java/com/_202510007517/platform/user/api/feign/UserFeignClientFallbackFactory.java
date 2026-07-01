package com._202510007517.platform.user.api.feign;

import com._202510007517.platform.common.exception.RemoteServerException;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    private static final String SERVICE_NAME = "user-service";

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public UserProfileDTO getProfile(Long userId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public UserProfileDTO updateProfile(Long userId, UpdateUserProfileDTO request) {
                throw serviceUnavailable(cause);
            }

            @Override
            public UserProfileDTO getProfileByUsername(String username) {
                throw serviceUnavailable(cause);
            }

            @Override
            public List<String> getRoles(Long userId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public UserRolesDTO getRolesDetail(Long userId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public StudentProfileDTO getStudentProfile(Long studentId) {
                throw serviceUnavailable(cause);
            }

            @Override
            public StudentProfileDTO updateStudentProfile(Long studentId, UpdateStudentProfileDTO request) {
                throw serviceUnavailable(cause);
            }

            @Override
            public List<UserProfileDTO> listByIds(List<Long> ids) {
                throw serviceUnavailable(cause);
            }
        };
    }

    private static RemoteServerException serviceUnavailable(Throwable cause) {
        String detail = cause != null && cause.getMessage() != null ? cause.getMessage() : "unknown";
        return new RemoteServerException(503, SERVICE_NAME + " 暂不可用", "{\"service\":\"" + SERVICE_NAME
                + "\",\"cause\":\"" + escape(detail) + "\"}");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
