package com._202510007517.platform.user.repository;

import com._202510007517.platform.user.domain.UserRecord;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserRecord> findById(Long id);

    Optional<UserRecord> findByUsername(String username);

    List<UserRecord> findByIds(List<Long> ids);

    List<String> findRolesByUserId(Long userId);

    String findStudentClassName(Long studentId);

    void updateProfile(UserRecord user);
}
