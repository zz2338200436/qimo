package com._202510007517.major_assignment.constants;

/**
 * 成功消息常量类
 * 统一管理系统中的成功提示信息
 */
public final class SuccessMessages {
    
    private SuccessMessages() {
        // 防止实例化
    }
    
    // 通用操作
    public static final String OPERATION_SUCCESS = "操作成功";
    public static final String CREATE_SUCCESS = "创建成功";
    public static final String UPDATE_SUCCESS = "更新成功";
    public static final String DELETE_SUCCESS = "删除成功";
    
    // 认证相关
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGOUT_SUCCESS = "退出成功";
    public static final String PASSWORD_CHANGED = "密码更新成功";
    
    // 数据获取
    public static final String GET_USER_INFO_SUCCESS = "获取用户信息成功";
    public static final String GET_COURSE_LIST_SUCCESS = "获取课程列表成功";
    public static final String GET_COURSE_DETAIL_SUCCESS = "获取课程详情成功";
    public static final String GET_ASSIGNMENT_LIST_SUCCESS = "获取作业列表成功";
    public static final String GET_ASSIGNMENT_DETAIL_SUCCESS = "获取作业详情成功";
    public static final String GET_EXAM_LIST_SUCCESS = "获取考试列表成功";
    public static final String GET_EXAM_DETAIL_SUCCESS = "获取考试详情成功";
    public static final String GET_STATS_SUCCESS = "获取学习统计成功";
    public static final String GET_KNOWLEDGE_POINTS_SUCCESS = "获取知识点列表成功";
    public static final String GET_KNOWLEDGE_POINT_DETAIL_SUCCESS = "获取知识点详情成功";
    public static final String GET_SCORES_SUCCESS = "获取成绩列表成功";
    public static final String GET_STUDY_TIME_SUCCESS = "获取学习时间分布成功";
    public static final String GET_EARLY_WARNINGS_SUCCESS = "获取学情预警列表成功";
    public static final String GET_STUDENTS_SUCCESS = "获取课程学生列表成功";
    public static final String GET_CLASS_NAME_SUCCESS = "获取班级名称成功";
    
    // 提交相关
    public static final String ASSIGNMENT_SUBMIT_SUCCESS = "作业提交成功";
    public static final String EXAM_SUBMIT_SUCCESS = "考试提交成功";
    
    // 设置相关
    public static final String PROFILE_UPDATE_SUCCESS = "个人信息更新成功";
    public static final String NOTIFICATION_SETTINGS_SUCCESS = "通知设置保存成功";
    public static final String PRIVACY_SETTINGS_SUCCESS = "隐私设置更新成功";
    public static final String AVATAR_UPLOAD_SUCCESS = "上传头像成功";
    public static final String EXPORT_DATA_SUCCESS = "导出数据成功";
}
