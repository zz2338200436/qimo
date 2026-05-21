package com._202510007517.platform.user.repository;

import com._202510007517.platform.user.domain.UserEntity;
import com._202510007517.platform.user.domain.UserRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    public JpaUserRepository(UserJpaRepository userJpaRepository, RoleJpaRepository roleJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRecord> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRecord> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> positions = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            positions.putIfAbsent(ids.get(i), i);
        }
        return userJpaRepository.findByIdIn(ids).stream()
                .sorted(Comparator.comparingInt(user -> positions.getOrDefault(user.getId(), Integer.MAX_VALUE)))
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findRolesByUserId(Long userId) {
        return roleJpaRepository.findRoleNamesByUserId(userId);
    }

    @Override
    public String findStudentClassName(Long studentId) {
        return "未知班级";
    }

    @Override
    @Transactional
    public void updateProfile(UserRecord user) {
        userJpaRepository.findById(user.getId()).ifPresent(entity -> {
            entity.setName(user.getName());
            entity.setEmail(user.getEmail());
            entity.setPhone(user.getPhone());
            entity.setAvatar(user.getAvatar());
            entity.setEnabled(user.isEnabled());
            userJpaRepository.save(entity);
        });
    }

    private UserRecord toRecord(UserEntity entity) {
        UserRecord user = new UserRecord();
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setName(entity.getName());
        user.setEmail(entity.getEmail());
        user.setPhone(entity.getPhone());
        user.setAvatar(entity.getAvatar());
        user.setEnabled(entity.isEnabled());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }
}
