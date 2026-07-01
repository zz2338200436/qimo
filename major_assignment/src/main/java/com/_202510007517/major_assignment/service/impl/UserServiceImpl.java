package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.mapper.UserMapper;
import com._202510007517.major_assignment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final String USER_WRITES_MOVED_MESSAGE =
            "用户写操作已迁移至 User_Service/Auth_Service，单体不再直接写 users 表";
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    
    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.USERS, key = "#id", unless = "#result == null")
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean login(String username, String password) {
        User user = userMapper.findByUsername(username);
        // 使用BCryptPasswordEncoder验证密码
        return user != null && bCryptPasswordEncoder.matches(password, user.getPassword());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.USER_ROLES, key = "#userId", unless = "#result == null")
    public List<String> getRolesByUserId(Long userId) {
        return userMapper.getRolesByUserId(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(User user) {
        throw new UnsupportedOperationException(USER_WRITES_MOVED_MESSAGE);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#user.id"),
        @CacheEvict(value = CacheConstants.USER_ROLES, key = "#user.id")
    })
    public void update(User user) {
        throw new UnsupportedOperationException(USER_WRITES_MOVED_MESSAGE);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.USERS, key = "#id"),
        @CacheEvict(value = CacheConstants.USER_ROLES, key = "#id")
    })
    public void delete(Long id) {
        throw new UnsupportedOperationException(USER_WRITES_MOVED_MESSAGE);
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getStudentClassName(Long studentId) {
        // 查询学生所在的班级名称
        return userMapper.getStudentClassName(studentId);
    }
}
