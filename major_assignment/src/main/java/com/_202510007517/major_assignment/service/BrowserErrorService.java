package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.BrowserError;
import com._202510007517.major_assignment.entity.dto.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 浏览器错误日志服务接口
 */
public interface BrowserErrorService {
    
    /**
     * 保存浏览器错误日志
     * @param browserError 浏览器错误日志实体
     * @return 保存成功的错误日志ID
     */
    Long saveBrowserError(BrowserError browserError);
    
    /**
     * 批量保存浏览器错误日志
     * @param browserErrors 浏览器错误日志实体列表
     * @return 保存成功的行数
     */
    int batchSaveBrowserErrors(List<BrowserError> browserErrors);
    
    /**
     * 根据ID查询浏览器错误日志
     * @param id 主键ID
     * @return 浏览器错误日志实体
     */
    BrowserError getBrowserErrorById(Long id);
    
    /**
     * 根据条件查询浏览器错误日志列表
     * @param params 查询条件
     * @param page 页码
     * @param size 每页数量
     * @return 浏览器错误日志实体列表
     */
    PageResult<BrowserError> getBrowserErrorList(Map<String, String> params, int page, int size);
    
    /**
     * 更新浏览器错误日志
     * @param browserError 浏览器错误日志实体
     * @return 更新成功的行数
     */
    int updateBrowserError(BrowserError browserError);
    
    /**
     * 根据ID更新错误状态
     * @param id 主键ID
     * @param status 错误状态
     * @param processedBy 处理人ID
     * @param processResult 处理结果
     * @return 更新成功的行数
     */
    int updateErrorStatus(Long id, Integer status, Long processedBy, String processResult);
    
    /**
     * 根据ID删除浏览器错误日志
     * @param id 主键ID
     * @return 删除成功的行数
     */
    int deleteBrowserErrorById(Long id);
    
    /**
     * 统计不同类型的错误数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误类型统计结果
     */
    List<Map<String, Object>> getErrorTypeStats(String startTime, String endTime);
    
    /**
     * 统计不同页面的错误数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回的记录数量
     * @return 页面错误统计结果
     */
    List<Map<String, Object>> getPageErrorStats(String startTime, String endTime, int limit);
}
