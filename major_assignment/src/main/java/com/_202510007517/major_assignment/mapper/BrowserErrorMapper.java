package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.BrowserError;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 浏览器错误日志Mapper接口
 */
@Mapper
public interface BrowserErrorMapper {
    
    /**
     * 插入浏览器错误日志
     * @param browserError 浏览器错误日志实体
     * @return 插入成功的行数
     */
    int insert(BrowserError browserError);
    
    /**
     * 批量插入浏览器错误日志
     * @param browserErrors 浏览器错误日志列表
     * @return 插入成功的行数
     */
    int batchInsert(@Param("browserErrors") List<BrowserError> browserErrors);
    
    /**
     * 根据ID查询浏览器错误日志
     * @param id 主键ID
     * @return 浏览器错误日志实体
     */
    BrowserError selectById(@Param("id") Long id);
    
    /**
     * 根据条件查询浏览器错误日志列表
     * @param params 查询条件
     * @return 浏览器错误日志列表
     */
    List<BrowserError> selectByParams(@Param("params") Map<String, Object> params);
    
    /**
     * 根据条件查询浏览器错误日志数量
     * @param params 查询条件
     * @return 错误日志数量
     */
    int countByParams(@Param("params") Map<String, Object> params);
    
    /**
     * 更新浏览器错误日志
     * @param browserError 浏览器错误日志实体
     * @return 更新成功的行数
     */
    int update(BrowserError browserError);
    
    /**
     * 根据ID更新错误状态
     * @param id 主键ID
     * @param status 错误状态
     * @param processedBy 处理人ID
     * @param processResult 处理结果
     * @return 更新成功的行数
     */
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status, 
                        @Param("processedBy") Long processedBy, @Param("processResult") String processResult);
    
    /**
     * 根据ID删除浏览器错误日志
     * @param id 主键ID
     * @return 删除成功的行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据条件逻辑删除浏览器错误日志
     * @param params 删除条件
     * @return 删除成功的行数
     */
    int deleteByParams(@Param("params") Map<String, Object> params);
    
    /**
     * 统计不同类型的错误数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误类型统计结果
     */
    List<Map<String, Object>> countByErrorType(@Param("startTime") String startTime, @Param("endTime") String endTime);
    
    /**
     * 统计不同页面的错误数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回的记录数量
     * @return 页面错误统计结果
     */
    List<Map<String, Object>> countByPageUrl(@Param("startTime") String startTime, @Param("endTime") String endTime, @Param("limit") int limit);
}