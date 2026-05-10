package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseResult<User> getUserById(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }

        User user = userService.findById(id);
        if (user == null) {
            return ResponseResult.failure("用户不存在", 404);
        }

        return ResponseResult.success(user);
    }
}
