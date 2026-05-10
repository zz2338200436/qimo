package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> getRolesByUserId(Long userId);
    
    // 新增用户
    @Insert("INSERT INTO users(username, password, name, email, phone, avatar, enabled, created_at, updated_at) VALUES(#{username}, #{password}, #{name}, #{email}, #{phone}, #{avatar}, #{enabled}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    // 更新用户
    @Update("UPDATE users SET username = #{username}, password = #{password}, name = #{name}, email = #{email}, phone = #{phone}, avatar = #{avatar}, enabled = #{enabled}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(User user);
    
    // 删除用户
    @Delete("DELETE FROM users WHERE id = #{id}")
    void delete(Long id);
    
    // 获取学生的班级名称
    @Select("SELECT cc.class_name FROM class_students cs JOIN course_classes cc ON cs.class_id = cc.id WHERE cs.student_id = #{studentId} LIMIT 1")
    String getStudentClassName(Long studentId);
}