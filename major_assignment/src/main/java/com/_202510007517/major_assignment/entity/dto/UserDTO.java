package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.util.List;

/**
 * 用户DTO - 用于API响应，不包含密码等敏感信息
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private Boolean enabled;
    private List<String> roles;
}

