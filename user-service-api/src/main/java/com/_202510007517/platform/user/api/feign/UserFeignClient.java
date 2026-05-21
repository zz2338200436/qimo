package com._202510007517.platform.user.api.feign;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/users", fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/{userId}")
    UserProfileDTO getProfile(@PathVariable("userId") Long userId);

    @PutMapping("/{userId}")
    UserProfileDTO updateProfile(@PathVariable("userId") Long userId,
                                 @RequestBody UpdateUserProfileDTO request);

    @GetMapping("/by-username/{username}")
    UserProfileDTO getProfileByUsername(@PathVariable("username") String username);

    @GetMapping("/{userId}/roles")
    List<String> getRoles(@PathVariable("userId") Long userId);

    @GetMapping("/{userId}/roles-detail")
    UserRolesDTO getRolesDetail(@PathVariable("userId") Long userId);

    @GetMapping("/{studentId}/student-profile")
    StudentProfileDTO getStudentProfile(@PathVariable("studentId") Long studentId);

    @PutMapping("/{studentId}/student-profile")
    StudentProfileDTO updateStudentProfile(@PathVariable("studentId") Long studentId,
                                           @RequestBody UpdateStudentProfileDTO request);

    @GetMapping
    List<UserProfileDTO> listByIds(@RequestParam("ids") List<Long> ids);
}
