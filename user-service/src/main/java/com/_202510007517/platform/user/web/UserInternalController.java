package com._202510007517.platform.user.web;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserApplicationService userApplicationService;

    public UserInternalController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping("/{userId}")
    public UserProfileDTO getProfile(@PathVariable Long userId) {
        return userApplicationService.getProfile(userId);
    }

    @PutMapping("/{userId}")
    public UserProfileDTO updateProfile(@PathVariable Long userId,
                                        @RequestBody UpdateUserProfileDTO request) {
        return userApplicationService.updateProfile(userId, request);
    }

    @GetMapping("/by-username/{username}")
    public UserProfileDTO getProfileByUsername(@PathVariable String username) {
        return userApplicationService.getProfileByUsername(username);
    }

    @GetMapping("/{userId}/roles")
    public List<String> getRoles(@PathVariable Long userId) {
        return userApplicationService.getRoles(userId);
    }

    @GetMapping("/{userId}/roles-detail")
    public UserRolesDTO getRolesDetail(@PathVariable Long userId) {
        return userApplicationService.getRolesDetail(userId);
    }

    @GetMapping("/{studentId}/student-profile")
    public StudentProfileDTO getStudentProfile(@PathVariable Long studentId) {
        return userApplicationService.getStudentProfile(studentId);
    }

    @PutMapping("/{studentId}/student-profile")
    public StudentProfileDTO updateStudentProfile(@PathVariable Long studentId,
                                                  @RequestBody UpdateStudentProfileDTO request) {
        return userApplicationService.updateStudentProfile(studentId, request);
    }

    @GetMapping
    public List<UserProfileDTO> listByIds(@RequestParam("ids") List<Long> ids) {
        return userApplicationService.listByIds(ids);
    }
}
