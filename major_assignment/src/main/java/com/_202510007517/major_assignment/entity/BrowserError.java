package com._202510007517.major_assignment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 浏览器错误日志实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowserError {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 错误类型（如：Error, Warning, Info）
     */
    private String errorType;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 错误堆栈信息
     */
    private String errorStack;
    
    /**
     * 错误发生的页面URL
     */
    private String pageUrl;
    
    /**
     * 错误发生的行号
     */
    private Integer lineNumber;
    
    /**
     * 错误发生的列号
     */
    private Integer columnNumber;
    
    /**
     * 错误发生的文件URL
     */
    private String fileUrl;
    
    /**
     * 用户代理信息（浏览器版本、操作系统等）
     */
    private String userAgent;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 错误发生时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date errorTime;
    
    /**
     * 错误状态（0：未处理，1：已处理）
     */
    private Integer status;
    
    /**
     * 处理人ID
     */
    private Long processedBy;
    
    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processedTime;
    
    /**
     * 处理结果
     */
    private String processResult;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
    
    /**
     * 数据版本号
     */
    private Integer version;
    
    /**
     * 逻辑删除标志（0：正常，1：删除）
     */
    private Integer deleted;
    
    /**
     * 实体创建前的初始化方法
     */
    public void init() {
        Date now = new Date();
        this.createdTime = now;
        this.updatedTime = now;
        this.errorTime = now;
        this.status = 0;
        this.deleted = 0;
    }
    
    /**
     * 实体更新前的初始化方法
     */
    public void updateTime() {
        this.updatedTime = new Date();
    }
}