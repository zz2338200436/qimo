package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.User;
import java.util.List;

public interface UserService {
    User findByUsername(String username);
    User findById(Long id);
    boolean login(String username, String password);
    List<String> getRolesByUserId(Long userId);
    void create(User user);
    void update(User user);
    void delete(Long id);
    String getStudentClassName(Long studentId);
}