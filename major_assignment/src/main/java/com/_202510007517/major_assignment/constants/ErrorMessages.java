package com._202510007517.major_assignment.constants;

/**
 * 错误消息常量类
 * 统一管理系统中的错误提示信息
 */
public final class ErrorMessages {
    
    private ErrorMessages() {
        // 防止实例化
    }
    
    // 认证相关
    public static final String UNAUTHORIZED = "未授权，请重新登录";
    public static final String FORBIDDEN = "无权限访问";
    public static final String LOGIN_FAILED = "用户名或密码错误";
    public static final String ACCOUNT_DISABLED = "账户已被禁用";
    public static final String CAPTCHA_ERROR = "验证码错误或已过期";
    public static final String PASSWORD_MISMATCH = "两次输入的密码不一致";
    public static final String PASSWORD_WEAK = "密码强度不足，需包含字母和数字且不少于8位";
    public static final String PASSWORD_SAME = "新密码不能与当前密码相同";
    public static final String CURRENT_PASSWORD_ERROR = "当前密码错误";
    
    // 资源相关
    public static final String NOT_FOUND = "资源不存在";
    public static final String COURSE_NOT_FOUND = "课程不存在";
    public static final String ASSIGNMENT_NOT_FOUND = "作业不存在";
    public static final String EXAM_NOT_FOUND = "考试不存在";
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String STUDENT_NOT_FOUND = "学生不存在";
    public static final String KNOWLEDGE_POINT_NOT_FOUND = "知识点详情不存在";
    
    // 参数校验相关
    public static final String PARAM_INVALID = "参数验证失败";
    public static final String PARAM_BINDING_FAILED = "参数绑定失败";
    public static final String CONTENT_EMPTY = "内容不能为空";
    public static final String NAME_EMPTY = "姓名不能为空";
    public static final String EMAIL_INVALID = "邮箱格式不正确";
    public static final String PASSWORD_EMPTY = "密码不能为空";
    public static final String PASSWORD_TOO_SHORT = "密码长度至少8位";
    
    // 操作相关
    public static final String OPERATION_FAILED = "操作失败";
    public static final String SUBMIT_FAILED = "提交失败";
    public static final String UPDATE_FAILED = "更新失败";
    public static final String DELETE_FAILED = "删除失败";
    public static final String CREATE_FAILED = "创建失败";
    public static final String INTERNAL_ERROR = "服务器内部错误";

    // HTTP / 下游通信相关（Design §Error Handling §3 —— 异常映射表）
    public static final String REQUEST_BODY_INVALID = "请求体格式错误";
    public static final String METHOD_NOT_ALLOWED = "不支持的请求方法";
    public static final String DOWNSTREAM_ERROR = "下游服务异常";
    public static final String CIRCUIT_BREAKER_OPEN = "服务暂时不可用，请稍后重试";
    public static final String TIMEOUT = "请求超时";
    public static final String RATE_LIMITED = "请求过于频繁，请稍后再试";
}
