package com._202510007517.major_assignment.utils;

import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.AssignmentDTO;
import com._202510007517.major_assignment.entity.dto.CourseDTO;
import com._202510007517.major_assignment.entity.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO转换工具类
 * 用于Entity和DTO之间的转换
 */
@Component
public class DTOConverter {

    /**
     * Assignment转AssignmentDTO
     */
    public AssignmentDTO toAssignmentDTO(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setTitle(assignment.getTitle());
        dto.setDescription(assignment.getDescription());
        // 转换Date到LocalDateTime
        if (assignment.getDueDate() != null) {
            dto.setDueDate(assignment.getDueDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());
        }
        if (assignment.getPublishDate() != null) {
            dto.setPublishDate(assignment.getPublishDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());
        }
        dto.setIsActive(assignment.getIsActive());
        dto.setCourseId(assignment.getCourseId());
        dto.setTeacherId(assignment.getTeacherId());
        return dto;
    }

    /**
     * Assignment列表转AssignmentDTO列表
     */
    public List<AssignmentDTO> toAssignmentDTOList(List<Assignment> assignments) {
        if (assignments == null) {
            return null;
        }
        return assignments.stream()
                .map(this::toAssignmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * Course转CourseDTO
     */
    public CourseDTO toCourseDTO(Course course) {
        if (course == null) {
            return null;
        }
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCourseCode(course.getCourseCode());
        dto.setCourseName(course.getCourseName());
        dto.setCredit(course.getCredit());
        dto.setCourseCategory(course.getCourseCategory());
        dto.setDescription(course.getDescription());
        dto.setTotalHours(course.getTotalHours());
        dto.setTeacherId(course.getTeacherId());
        dto.setCourseStatus(course.getCourseStatus());
        dto.setSemester(course.getSemester());
        // Course实体中startDate和endDate是String类型
        if (course.getStartDate() != null && !course.getStartDate().isEmpty()) {
            try {
                dto.setStartDate(java.time.LocalDate.parse(course.getStartDate()));
            } catch (Exception e) {
                // 如果解析失败，保持为null
            }
        }
        if (course.getEndDate() != null && !course.getEndDate().isEmpty()) {
            try {
                dto.setEndDate(java.time.LocalDate.parse(course.getEndDate()));
            } catch (Exception e) {
                // 如果解析失败，保持为null
            }
        }
        dto.setMaxStudents(course.getMaxStudents());
        dto.setStudentCount(course.getStudentCount());
        return dto;
    }

    /**
     * Course列表转CourseDTO列表
     */
    public List<CourseDTO> toCourseDTOList(List<Course> courses) {
        if (courses == null) {
            return null;
        }
        return courses.stream()
                .map(this::toCourseDTO)
                .collect(Collectors.toList());
    }

    /**
     * User转UserDTO
     */
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setEnabled(user.getEnabled());
        // 注意：roles需要单独设置，因为User实体中没有roles字段
        return dto;
    }

    /**
     * User列表转UserDTO列表
     */
    public List<UserDTO> toUserDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }
}

