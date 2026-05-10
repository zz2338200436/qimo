package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.BrowserError;
import com._202510007517.major_assignment.mapper.BrowserErrorMapper;
import com._202510007517.major_assignment.service.BrowserErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览器错误日志服务实现类
 */
@Service
public class BrowserErrorServiceImpl implements BrowserErrorService {

    @Autowired
    private BrowserErrorMapper browserErrorMapper;

    @Override
    public Long saveBrowserError(BrowserError browserError) {
        browserError.init();
        browserErrorMapper.insert(browserError);
        return browserError.getId();
    }

    @Override
    public int batchSaveBrowserErrors(List<BrowserError> browserErrors) {
        browserErrors.forEach(BrowserError::init);
        return browserErrorMapper.batchInsert(browserErrors);
    }

    @Override
    public BrowserError getBrowserErrorById(Long id) {
        return browserErrorMapper.selectById(id);
    }

    @Override
    public List<BrowserError> getBrowserErrorList(Map<String, String> params, int page, int size) {
        // 转换为Map<String, Object>以便添加分页参数
        Map<String, Object> queryParams = new HashMap<>(params);
        // 计算分页参数
        queryParams.put("offset", (page - 1) * size);
        queryParams.put("limit", size);
        return browserErrorMapper.selectByParams(queryParams);
    }

    @Override
    public int getBrowserErrorCount(Map<String, String> params) {
        // 转换为Map<String, Object>以便传递给Mapper
        Map<String, Object> queryParams = new HashMap<>(params);
        return browserErrorMapper.countByParams(queryParams);
    }

    @Override
    public int updateBrowserError(BrowserError browserError) {
        browserError.updateTime();
        return browserErrorMapper.update(browserError);
    }

    @Override
    public int updateErrorStatus(Long id, Integer status, Long processedBy, String processResult) {
        return browserErrorMapper.updateStatusById(id, status, processedBy, processResult);
    }

    @Override
    public int deleteBrowserErrorById(Long id) {
        return browserErrorMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getErrorTypeStats(String startTime, String endTime) {
        return browserErrorMapper.countByErrorType(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getPageErrorStats(String startTime, String endTime, int limit) {
        return browserErrorMapper.countByPageUrl(startTime, endTime, limit);
    }
}
